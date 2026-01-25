package com.itda.backend.worker.image;

public interface ImageStorage {

    ImageStorageResult save(Long projectId, Long jobId, byte[] bytes, String contentType);
}
