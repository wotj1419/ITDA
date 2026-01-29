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
        Objects.requireNonNull(storedAsset, "StoredAsset is required");
        return registerAsset(
                job,
                storedAsset.storageKey(),
                storedAsset.sizeBytes(),
                assetType,
                contentType,
                StorageProvider.LOCAL
        );
    }

    public Long registerAsset(
            Job job,
            String storageKey,
            long sizeBytes,
            AssetType assetType,
            String contentType,
            StorageProvider storageProvider
    ) {
        requireJob(job);
        requireProjectId(job);
        Objects.requireNonNull(storageKey, "storageKey is required");
        Objects.requireNonNull(assetType, "assetType is required");
        Objects.requireNonNull(storageProvider, "storageProvider is required");

        Asset asset = Asset.builder()
                .ownerId(resolveOwnerId(job))
                .projectId(job.getProjectId())
                .assetType(assetType)
                .storageProvider(storageProvider)
                .storageKey(storageKey)
                .contentType(contentType)
                .sizeBytes(sizeBytes)
                .build();
        assetMapper.insert(asset);
        return asset.getId();
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
