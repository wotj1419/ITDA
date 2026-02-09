package com.itda.backend.worker;

public record ExecutionResult(
        Long resultAssetId,
        String nodeContentKey,
        Long thumbnailAssetId,
        String thumbnailContentKey,
        Integer durationMs
) {
    public ExecutionResult(Long resultAssetId, String nodeContentKey) {
        this(resultAssetId, nodeContentKey, null, null, null);
    }

    public ExecutionResult(Long resultAssetId,
                           String nodeContentKey,
                           Long thumbnailAssetId,
                           String thumbnailContentKey) {
        this(resultAssetId, nodeContentKey, thumbnailAssetId, thumbnailContentKey, null);
    }
}
