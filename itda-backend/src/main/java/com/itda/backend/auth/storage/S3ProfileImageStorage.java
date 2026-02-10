package com.itda.backend.auth.storage;

import com.itda.backend.asset.config.S3StorageProperties;
import com.itda.backend.asset.domain.StorageProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.provider", havingValue = "S3")
public class S3ProfileImageStorage implements ProfileImageStorage {

    private static final String DEFAULT_CONTENT_TYPE = "image/png";
    private static final String DEFAULT_EXTENSION = ".png";

    private final S3Client s3Client;
    private final S3StorageProperties s3Properties;

    @Override
    public ProfileImageStorageResult save(Long userId, byte[] bytes, String contentType) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("image bytes must not be empty");
        }

        String bucket = requireBucket();
        String normalizedContentType = normalizeContentType(contentType);
        String extension = resolveExtension(normalizedContentType);
        String storageKey = buildStorageKey(userId, extension);

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(storageKey)
                .contentType(normalizedContentType)
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(bytes));

        log.info("[S3ProfileImageStorage] Stored profile image: bucket={}, key={}", bucket, storageKey);
        return new ProfileImageStorageResult(storageKey, normalizedContentType, bytes.length, StorageProvider.S3);
    }

    private String buildStorageKey(Long userId, String extension) {
        String uuid = UUID.randomUUID().toString();
        return String.format("profile-images/%d/%s%s", userId, uuid, extension)
                .replace("\\", "/");
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
        if ("image/webp".equals(contentType)) {
            return ".webp";
        }
        return DEFAULT_EXTENSION;
    }
}
