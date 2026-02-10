package com.itda.backend.auth.storage;

public interface ProfileImageStorage {
    ProfileImageStorageResult save(Long userId, byte[] bytes, String contentType);
}
