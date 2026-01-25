package com.itda.backend.worker.image;

import com.itda.backend.asset.domain.StorageProvider;

public record ImageStorageResult(
        String storageKey,
        String contentType,
        long sizeBytes,
        StorageProvider storageProvider
) {
}
