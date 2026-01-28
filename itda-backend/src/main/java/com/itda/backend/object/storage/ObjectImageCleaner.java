package com.itda.backend.object.storage;

import com.itda.backend.asset.config.S3StorageProperties;
import com.itda.backend.asset.domain.StorageProvider;
import com.itda.backend.worker.LocalFileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class ObjectImageCleaner {

    private final LocalFileStorage localFileStorage;
    private final ObjectProvider<S3Client> s3ClientProvider;
    private final S3StorageProperties s3Properties;

    public void delete(StorageProvider storageProvider, String storageKey) {
        if (storageProvider == null || storageKey == null || storageKey.isBlank()) {
            return;
        }
        try {
            if (storageProvider == StorageProvider.LOCAL) {
                localFileStorage.delete(storageKey);
                return;
            }
            if (storageProvider == StorageProvider.S3) {
                deleteFromS3(storageKey);
            }
        } catch (Exception e) {
            log.warn("[ObjectImageCleaner] Failed to delete object image: provider={}, key={}", storageProvider, storageKey, e);
        }
    }

    private void deleteFromS3(String storageKey) {
        S3Client s3Client = s3ClientProvider.getIfAvailable();
        if (s3Client == null) {
            log.warn("[ObjectImageCleaner] S3 client not available. skip delete: key={}", storageKey);
            return;
        }
        String bucket = s3Properties.getBucket();
        if (bucket == null || bucket.trim().isEmpty()) {
            log.warn("[ObjectImageCleaner] S3 bucket not configured. skip delete: key={}", storageKey);
            return;
        }
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket.trim())
                .key(storageKey)
                .build());
    }
}
