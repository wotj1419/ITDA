package com.itda.backend.auth.storage;

import com.itda.backend.asset.domain.StorageProvider;
import com.itda.backend.worker.LocalFileStorage;
import com.itda.backend.worker.StoredAsset;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.provider", havingValue = "LOCAL", matchIfMissing = true)
public class LocalProfileImageStorage implements ProfileImageStorage {

    private static final String DEFAULT_CONTENT_TYPE = "image/png";
    private static final String DEFAULT_EXTENSION = ".png";

    private final LocalFileStorage localFileStorage;

    @Override
    public ProfileImageStorageResult save(Long userId, byte[] bytes, String contentType) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("image bytes must not be empty");
        }

        String normalizedContentType = normalizeContentType(contentType);
        String extension = resolveExtension(normalizedContentType);
        String storageKey = buildStorageKey(userId, extension);

        StoredAsset storedAsset = localFileStorage.save(storageKey, bytes);
        return new ProfileImageStorageResult(
                storedAsset.storageKey(),
                normalizedContentType,
                storedAsset.sizeBytes(),
                StorageProvider.LOCAL
        );
    }

    private String buildStorageKey(Long userId, String extension) {
        String uuid = UUID.randomUUID().toString();
        return String.format("profile-images/%d/%s%s", userId, uuid, extension)
                .replace("\\", "/");
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
