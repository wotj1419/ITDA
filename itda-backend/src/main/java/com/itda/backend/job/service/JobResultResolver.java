package com.itda.backend.job.service;

import com.itda.backend.asset.config.S3StorageProperties;
import com.itda.backend.asset.domain.Asset;
import com.itda.backend.asset.domain.StorageProvider;
import com.itda.backend.asset.repository.AssetMapper;
import com.itda.backend.job.domain.Job;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;

/**
 * Job 결과 URL 해석기
 * <p>
 * Job 결과(Asset ID)를 presigned URL로 변환.
 * 현재는 스텁 구현이며, AssetService 연동 시 실제 구현 필요.
 */
@Component
@RequiredArgsConstructor
public class JobResultResolver {

    private final AssetMapper assetMapper;
    private final S3StorageProperties s3Properties;
    private final ObjectProvider<S3Presigner> s3PresignerProvider;

    /**
     * Job 결과를 presigned URL로 변환
     *
     * @param job 대상 Job
     * @return presigned URL (결과가 없으면 null)
     */
    public String resolve(Job job) {
        if (job.getResultAssetId() == null) {
            return null;
        }

        Asset asset = assetMapper.findById(job.getResultAssetId()).orElse(null);
        if (asset == null) {
            return null;
        }
        if (asset.getStorageProvider() != StorageProvider.S3) {
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
