package com.itda.backend.auth.storage;

import com.itda.backend.asset.domain.StorageProvider;

public record ProfileImageStorageResult(
        String storageKey,
        String contentType,
        long sizeBytes,
        StorageProvider storageProvider
) {
}
