package com.itda.backend.worker.video;

import com.itda.backend.asset.config.S3StorageProperties;
import com.itda.backend.asset.domain.StorageProvider;
import com.itda.backend.global.config.FileStorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoContentLoader {

    private static final String TEMP_INPUT_DIR = "tmp/merge-inputs";
    private static final String DEFAULT_EXTENSION = ".mp4";

    private final FileStorageProperties fileStorageProperties;
    private final ObjectProvider<S3Client> s3ClientProvider;
    private final S3StorageProperties s3Properties;

    public VideoInput load(String contentKey, StorageProvider provider, String context) {
        if (!hasText(contentKey)) {
            throw new IllegalStateException("Video content key missing: " + context);
        }

        StorageProvider resolved = provider == null ? StorageProvider.LOCAL : provider;

        VideoInput input = null;
        if (resolved == StorageProvider.S3) {
            input = tryLoadFromS3(contentKey, context);
            if (input != null) {
                return input;
            }
        }

        input = tryLoadFromLocal(contentKey);
        if (input != null) {
            return input;
        }

        if (resolved != StorageProvider.S3) {
            input = tryLoadFromS3(contentKey, context);
            if (input != null) {
                return input;
            }
        }

        throw new IllegalStateException("Video file missing: " + context + ", key=" + contentKey);
    }

    private VideoInput tryLoadFromLocal(String contentKey) {
        Path targetPath = resolveUnderUploadRoot(contentKey);
        if (!Files.exists(targetPath) || !Files.isReadable(targetPath)) {
            return null;
        }
        return new VideoInput(targetPath, false);
    }

    private VideoInput tryLoadFromS3(String contentKey, String context) {
        S3Client s3Client = s3ClientProvider.getIfAvailable();
        if (s3Client == null || !hasText(s3Properties.getBucket())) {
            return null;
        }
        String bucket = s3Properties.getBucket().trim();
        Path tempFile = null;
        try {
            Path tempDir = resolveTempDir();
            String extension = resolveExtension(contentKey);
            tempFile = Files.createTempFile(tempDir, "merge-input-", extension);
            try (ResponseInputStream<GetObjectResponse> stream = s3Client.getObject(GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(contentKey)
                    .build())) {
                Files.copy(stream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }
            return new VideoInput(tempFile, true);
        } catch (NoSuchKeyException e) {
            deleteQuietly(tempFile);
            return null;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                deleteQuietly(tempFile);
                return null;
            }
            log.warn("[VideoContentLoader] S3 load failed: key={}, status={}, context={}",
                    contentKey, e.statusCode(), context);
            deleteQuietly(tempFile);
            throw new IllegalStateException("Failed to load video content from S3: key=" + contentKey, e);
        } catch (IOException e) {
            deleteQuietly(tempFile);
            throw new IllegalStateException("Failed to read video content from S3: key=" + contentKey, e);
        }
    }

    private Path resolveUnderUploadRoot(String relativePath) {
        Path root = Path.of(fileStorageProperties.getUploadDir())
                .toAbsolutePath()
                .normalize();
        Path target = root.resolve(relativePath).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalStateException("Invalid content path");
        }
        return target;
    }

    private Path resolveTempDir() throws IOException {
        Path root = Path.of(fileStorageProperties.getUploadDir())
                .toAbsolutePath()
                .normalize();
        Path tempDir = root.resolve(TEMP_INPUT_DIR).normalize();
        if (!tempDir.startsWith(root)) {
            throw new IllegalStateException("Invalid temp dir");
        }
        Files.createDirectories(tempDir);
        return tempDir;
    }

    private String resolveExtension(String contentKey) {
        if (contentKey == null) {
            return DEFAULT_EXTENSION;
        }
        String trimmed = contentKey.trim();
        int slash = trimmed.lastIndexOf('/');
        String fileName = slash >= 0 ? trimmed.substring(slash + 1) : trimmed;
        int dot = fileName.lastIndexOf('.');
        if (dot > 0 && dot < fileName.length() - 1) {
            String ext = fileName.substring(dot);
            if (ext.length() <= 6) {
                return ext.startsWith(".") ? ext : "." + ext;
            }
        }
        return DEFAULT_EXTENSION;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }
}
