package com.itda.backend.worker.image;

import com.itda.backend.asset.domain.StorageProvider;
import com.itda.backend.worker.LocalFileStorage;
import com.itda.backend.worker.LocalJobAssetStorage;
import com.itda.backend.worker.StoredAsset;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "storage.provider", havingValue = "LOCAL", matchIfMissing = true)
public class LocalImageStorage extends LocalJobAssetStorage implements ImageStorage {

    private static final String DEFAULT_CONTENT_TYPE = "image/png";

    public LocalImageStorage(LocalFileStorage localFileStorage) {
        super(localFileStorage, "image", "ai/images", ".png");
    }

    @Override
    public ImageStorageResult save(Long projectId, Long jobId, byte[] bytes, String contentType) {
        StoredAsset storedAsset = super.save(projectId, jobId, bytes);
        String resolvedContentType = normalizeContentType(contentType);
        return new ImageStorageResult(
                storedAsset.storageKey(),
                resolvedContentType,
                storedAsset.sizeBytes(),
                StorageProvider.LOCAL
        );
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
