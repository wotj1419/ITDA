package com.itda.backend.worker.video;

import com.itda.backend.asset.config.S3StorageProperties;
import com.itda.backend.asset.domain.Asset;
import com.itda.backend.asset.domain.AssetType;
import com.itda.backend.worker.AssetRegistrar;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Component
@RequiredArgsConstructor
@Primary
@ConditionalOnProperty(name = "storage.provider", havingValue = "S3")
public class S3VideoStorage implements VideoStorage {

    private static final String DEFAULT_CONTENT_TYPE = "video/mp4";

    private final S3Client s3Client;
    private final S3StorageProperties s3Properties;
    private final AssetRegistrar assetRegistrar;

    @Override
    public Asset storeMergedVideo(Path localFile, String storageKey) {
        requireLocalFile(localFile);
        String normalizedKey = normalizeStorageKey(storageKey);
        String bucket = requireBucket();
        String contentType = resolveContentType(localFile);
        long sizeBytes = resolveSize(localFile);

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(normalizedKey)
                .contentType(contentType)
                .build();

        s3Client.putObject(request, RequestBody.fromFile(localFile));

        Long projectId = resolveProjectId(normalizedKey);
        Asset asset = assetRegistrar.registerS3Asset(
                projectId,
                null,
                normalizedKey,
                AssetType.VIDEO,
                contentType,
                sizeBytes
        );

        log.info("[S3VideoStorage] Stored video: bucket={}, key={}, assetId={}", bucket, normalizedKey, asset.getId());
        return asset;
    }

    private void requireLocalFile(Path localFile) {
        if (localFile == null) {
            throw new IllegalArgumentException("localFile is required");
        }
        if (!Files.exists(localFile)) {
            throw new IllegalStateException("localFile does not exist");
        }
    }

    private String normalizeStorageKey(String storageKey) {
        if (storageKey == null || storageKey.trim().isEmpty()) {
            throw new IllegalArgumentException("storageKey is required");
        }
        String normalized = storageKey.trim().replace("\\", "/");
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private String requireBucket() {
        String bucket = s3Properties.getBucket();
        if (bucket == null || bucket.trim().isEmpty()) {
            throw new IllegalStateException("storage.s3.bucket must be configured");
        }
        return bucket.trim();
    }

    private String resolveContentType(Path localFile) {
        try {
            String contentType = Files.probeContentType(localFile);
            if (contentType == null || contentType.isBlank()) {
                return DEFAULT_CONTENT_TYPE;
            }
            String lowered = contentType.toLowerCase();
            int semicolon = lowered.indexOf(';');
            if (semicolon > 0) {
                lowered = lowered.substring(0, semicolon).trim();
            }
            return lowered.isEmpty() ? DEFAULT_CONTENT_TYPE : lowered;
        } catch (IOException e) {
            return DEFAULT_CONTENT_TYPE;
        }
    }

    private long resolveSize(Path localFile) {
        try {
            return Files.size(localFile);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to resolve file size", e);
        }
    }

    private Long resolveProjectId(String storageKey) {
        String normalized = normalizeStorageKey(storageKey);
        String[] parts = normalized.split("/");
        if (parts.length >= 2 && "projects".equals(parts[0])) {
            return parseLongSafely(parts[1]);
        }
        if (parts.length >= 3 && "ai".equals(parts[0]) && "videos".equals(parts[1])) {
            return parseLongSafely(parts[2]);
        }
        return null;
    }

    private Long parseLongSafely(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
