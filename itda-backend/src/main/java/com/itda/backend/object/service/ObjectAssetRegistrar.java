package com.itda.backend.object.service;

import com.itda.backend.asset.domain.Asset;
import com.itda.backend.asset.domain.AssetType;
import com.itda.backend.asset.domain.StorageProvider;
import com.itda.backend.asset.repository.AssetMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class ObjectAssetRegistrar {

    private final AssetMapper assetMapper;

    public Long registerObjectImage(
            Long ownerId,
            Long projectId,
            String storageKey,
            long sizeBytes,
            String contentType,
            StorageProvider storageProvider
    ) {
        Objects.requireNonNull(projectId, "projectId is required");
        Objects.requireNonNull(storageKey, "storageKey is required");
        Objects.requireNonNull(storageProvider, "storageProvider is required");

        Asset asset = Asset.builder()
                .ownerId(ownerId)
                .projectId(projectId)
                .assetType(AssetType.IMAGE)
                .storageProvider(storageProvider)
                .storageKey(storageKey)
                .contentType(contentType)
                .sizeBytes(sizeBytes)
                .build();
        assetMapper.insert(asset);
        return asset.getId();
    }
}
