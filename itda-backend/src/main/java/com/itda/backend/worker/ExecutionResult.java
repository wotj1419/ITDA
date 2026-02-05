package com.itda.backend.worker;

public record ExecutionResult(
        Long resultAssetId,
        String nodeContentKey,
        Long thumbnailAssetId,
        String thumbnailContentKey
) {
    public ExecutionResult(Long resultAssetId, String nodeContentKey) {
        this(resultAssetId, nodeContentKey, null, null);
    }
}
