package com.itda.backend.worker;

public abstract class LocalJobAssetStorage {

    private final LocalFileStorage localFileStorage;
    private final String assetLabel;
    private final String baseDir;
    private final String fileExtension;

    protected LocalJobAssetStorage(
            LocalFileStorage localFileStorage,
            String assetLabel,
            String baseDir,
            String fileExtension
    ) {
        this.localFileStorage = localFileStorage;
        this.assetLabel = assetLabel;
        this.baseDir = baseDir;
        this.fileExtension = fileExtension;
    }

    public StoredAsset save(Long projectId, Long jobId, byte[] bytes) {
        requireJobIdentifiers(projectId, jobId);
        String relativePath = baseDir + "/" + projectId + "/job-" + jobId + fileExtension;
        return localFileStorage.save(relativePath, bytes);
    }

    private void requireJobIdentifiers(Long projectId, Long jobId) {
        if (projectId == null || jobId == null) {
            throw new IllegalStateException("Missing projectId/jobId for " + assetLabel + " storage");
        }
    }
}
