package com.itda.backend.worker;

import com.itda.backend.asset.domain.Asset;
import com.itda.backend.asset.domain.AssetType;
import com.itda.backend.asset.domain.StorageProvider;
import com.itda.backend.asset.repository.AssetMapper;
import com.itda.backend.job.domain.Job;
import com.itda.backend.node.domain.Node;
import com.itda.backend.node.repository.NodeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class AssetRegistrar {

    private final AssetMapper assetMapper;
    private final NodeMapper nodeMapper;

    public Long registerLocalAsset(Job job, StoredAsset storedAsset, AssetType assetType, String contentType) {
        requireJob(job);
        requireProjectId(job);
        Objects.requireNonNull(storedAsset, "StoredAsset is required");

        Asset asset = Asset.builder()
                .ownerId(resolveOwnerId(job))
                .projectId(job.getProjectId())
                .assetType(assetType)
                .storageProvider(StorageProvider.LOCAL)
                .storageKey(storedAsset.storageKey())
                .contentType(contentType)
                .sizeBytes(storedAsset.sizeBytes())
                .build();
        assetMapper.insert(asset);
        return asset.getId();
    }

    public Asset registerS3Asset(Long projectId,
                                 Long ownerId,
                                 String storageKey,
                                 AssetType assetType,
                                 String contentType,
                                 long sizeBytes) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("storageKey is required");
        }
        Objects.requireNonNull(assetType, "assetType is required");

        Asset asset = Asset.builder()
                .ownerId(ownerId)
                .projectId(projectId)
                .assetType(assetType)
                .storageProvider(StorageProvider.S3)
                .storageKey(storageKey)
                .contentType(contentType)
                .sizeBytes(sizeBytes)
                .build();
        assetMapper.insert(asset);
        return asset;
    }

    public Asset registerLocalAsset(Long projectId,
                                    Long ownerId,
                                    String storageKey,
                                    AssetType assetType,
                                    String contentType,
                                    long sizeBytes) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("storageKey is required");
        }
        Objects.requireNonNull(assetType, "assetType is required");

        Asset asset = Asset.builder()
                .ownerId(ownerId)
                .projectId(projectId)
                .assetType(assetType)
                .storageProvider(StorageProvider.LOCAL)
                .storageKey(storageKey)
                .contentType(contentType)
                .sizeBytes(sizeBytes)
                .build();
        assetMapper.insert(asset);
        return asset;
    }

    private void requireJob(Job job) {
        if (job == null) {
            throw new IllegalArgumentException("Job is required");
        }
    }

    private void requireProjectId(Job job) {
        if (job.getProjectId() == null) {
            throw new IllegalStateException("Job missing projectId");
        }
    }

    private Long resolveOwnerId(Job job) {
        if (job.getNodeId() == null) {
            return null;
        }
        return nodeMapper.findById(job.getNodeId())
                .map(Node::getCreatedBy)
                .orElse(null);
    }
}
