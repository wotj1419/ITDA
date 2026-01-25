package com.itda.backend.worker.image;

import com.itda.backend.asset.config.S3StorageProperties;
import com.itda.backend.asset.domain.StorageProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.provider", havingValue = "S3")
public class S3ImageStorage implements ImageStorage {

    private static final String DEFAULT_CONTENT_TYPE = "image/png";
    private static final String DEFAULT_EXTENSION = ".png";

    private final S3Client s3Client;
    private final S3StorageProperties s3Properties;

    @Override
    public ImageStorageResult save(Long projectId, Long jobId, byte[] bytes, String contentType) {
        if (projectId == null || jobId == null) {
            throw new IllegalArgumentException("projectId/jobId must not be null");
        }
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("image bytes must not be empty");
        }

        String bucket = requireBucket();
        String normalizedContentType = normalizeContentType(contentType);
        String extension = resolveExtension(normalizedContentType);
        String storageKey = String.format("ai/image/%d/job-%d%s", projectId, jobId, extension)
                .replace("\\", "/");

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(storageKey)
                .contentType(normalizedContentType)
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(bytes));

        log.info("[S3ImageStorage] Stored image: bucket={}, key={}", bucket, storageKey);
        return new ImageStorageResult(storageKey, normalizedContentType, bytes.length, StorageProvider.S3);
    }

    private String requireBucket() {
        String bucket = s3Properties.getBucket();
        if (bucket == null || bucket.trim().isEmpty()) {
            throw new IllegalStateException("storage.s3.bucket must be configured");
        }
        return bucket.trim();
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null) {
            return DEFAULT_CONTENT_TYPE;
        }
        String trimmed = contentType.trim();
        if (trimmed.isEmpty()) {
            return DEFAULT_CONTENT_TYPE;
        }
        String lowered = trimmed.toLowerCase();
        int semicolon = lowered.indexOf(';');
        if (semicolon > 0) {
            lowered = lowered.substring(0, semicolon).trim();
        }
        return lowered.isEmpty() ? DEFAULT_CONTENT_TYPE : lowered;
    }

    private String resolveExtension(String contentType) {
        if ("image/jpeg".equals(contentType) || "image/jpg".equals(contentType)) {
            return ".jpg";
        }
        if ("image/png".equals(contentType)) {
            return ".png";
        }
        return DEFAULT_EXTENSION;
    }
}
