package com.itda.backend.worker.video;

import com.itda.backend.asset.domain.Asset;
import com.itda.backend.asset.domain.AssetType;
import com.itda.backend.asset.domain.StorageProvider;
import com.itda.backend.worker.AssetRegistrar;
import com.itda.backend.worker.LocalFileStorage;
import com.itda.backend.worker.LocalJobAssetStorage;
import com.itda.backend.worker.StoredAsset;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@ConditionalOnProperty(name = "storage.provider", havingValue = "LOCAL", matchIfMissing = true)
public class LocalVideoStorage extends LocalJobAssetStorage implements VideoStorage {

    private static final String DEFAULT_CONTENT_TYPE = "video/mp4";

    private final LocalFileStorage localFileStorage;
    private final AssetRegistrar assetRegistrar;

    public LocalVideoStorage(LocalFileStorage localFileStorage, AssetRegistrar assetRegistrar) {
        super(localFileStorage, "video", "ai/videos", ".mp4");
        this.localFileStorage = localFileStorage;
        this.assetRegistrar = assetRegistrar;
    }

    @Override
    public Asset storeMergedVideo(Path localFile, String storageKey) {
        requireLocalFile(localFile);
        String normalizedKey = normalizeStorageKey(storageKey);

        StoredAsset storedAsset = localFileStorage.save(localFile, normalizedKey);
        String contentType = resolveContentType(localFile);
        Long projectId = resolveProjectId(normalizedKey);

        return assetRegistrar.registerLocalAsset(
                projectId,
                null,
                storedAsset.storageKey(),
                AssetType.VIDEO,
                contentType,
                storedAsset.sizeBytes()
        );
    }

    @Override
    public VideoStorageResult save(Long projectId, Long jobId, byte[] bytes, String contentType) {
        StoredAsset storedAsset = super.save(projectId, jobId, bytes);
        String resolvedContentType = normalizeContentType(contentType);
        return new VideoStorageResult(
                storedAsset.storageKey(),
                resolvedContentType,
                storedAsset.sizeBytes(),
                StorageProvider.LOCAL
        );
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

    private Long resolveProjectId(String storageKey) {
        String[] parts = storageKey.split("/");
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

    private String normalizeContentType(String contentType) {
        if (contentType == null) {
            return DEFAULT_CONTENT_TYPE;
        }
        String trimmed = contentType.trim();
        if (trimmed.isEmpty()) {
            return DEFAULT_CONTENT_TYPE;
        }
        return trimmed;
    }
}
