package com.itda.backend.object.storage;

import com.itda.backend.asset.domain.StorageProvider;

public record ObjectImageStorageResult(
        String storageKey,
        String contentType,
        long sizeBytes,
        StorageProvider storageProvider
) {
}
