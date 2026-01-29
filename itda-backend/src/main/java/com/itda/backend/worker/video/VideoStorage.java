package com.itda.backend.worker.video;

public interface VideoStorage {

    VideoStorageResult save(Long projectId, Long jobId, byte[] bytes, String contentType);
}
