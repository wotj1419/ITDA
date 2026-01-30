package com.itda.backend.object.storage;

public interface ObjectImageStorage {

    ObjectImageStorageResult save(Long projectId, Long objectId, byte[] bytes, String contentType);
}
