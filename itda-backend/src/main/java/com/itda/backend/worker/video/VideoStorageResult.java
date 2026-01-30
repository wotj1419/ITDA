package com.itda.backend.worker.video;

import com.itda.backend.asset.domain.StorageProvider;

public record VideoStorageResult(
        String storageKey,
        String contentType,
        long sizeBytes,
        StorageProvider storageProvider
) {
}
