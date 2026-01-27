package com.itda.backend.asset.service;

import com.itda.backend.asset.config.S3StorageProperties;
import com.itda.backend.asset.domain.Asset;
import com.itda.backend.asset.domain.StorageProvider;
import com.itda.backend.asset.repository.AssetMapper;
import com.itda.backend.media.MediaUrlResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class AssetUrlResolver {

    private final AssetMapper assetMapper;
    private final S3StorageProperties s3Properties;
    private final ObjectProvider<S3Presigner> s3PresignerProvider;
    private final MediaUrlResolver mediaUrlResolver;

    public String resolveNodeUrl(Long assetId, Long nodeId, String fallbackUrl) {
        String presignedUrl = resolveS3PresignedUrl(assetId);
        if (presignedUrl != null) {
            return presignedUrl;
        }
        return mediaUrlResolver.nodeContentUrl(nodeId, fallbackUrl);
    }

    public String resolveUrl(Long assetId, String fallbackUrl) {
        String presignedUrl = resolveS3PresignedUrl(assetId);
        return presignedUrl != null ? presignedUrl : fallbackUrl;
    }

    private String resolveS3PresignedUrl(Long assetId) {
        if (assetId == null) {
            return null;
        }
        Asset asset = assetMapper.findById(assetId).orElse(null);
        if (asset == null || asset.getStorageProvider() != StorageProvider.S3) {
            return null;
        }

        String storageKey = asset.getStorageKey();
        if (storageKey == null || storageKey.isBlank()) {
            return null;
        }

        S3Presigner presigner = s3PresignerProvider.getIfAvailable();
        if (presigner == null) {
            return null;
        }

        String bucket = s3Properties.getBucket();
        if (bucket == null || bucket.isBlank()) {
            return null;
        }

        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucket.trim())
                .key(storageKey)
                .build();

        long expires = Math.max(60, s3Properties.getPresignExpireSeconds());
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(expires))
                .getObjectRequest(getRequest)
                .build();

        return presigner.presignGetObject(presignRequest).url().toString();
    }
}
