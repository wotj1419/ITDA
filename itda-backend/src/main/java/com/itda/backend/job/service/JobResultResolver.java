package com.itda.backend.job.service;

import com.itda.backend.job.domain.Job;
import org.springframework.stereotype.Component;

/**
 * Job 결과 URL 해석기
 * <p>
 * Job 결과(Asset ID)를 presigned URL로 변환.
 * 현재는 스텁 구현이며, AssetService 연동 시 실제 구현 필요.
 */
@Component
public class JobResultResolver {

    // TODO: AssetService 주입 후 presigned URL 생성 로직 구현
    // private final AssetService assetService;

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

        // TODO: AssetService에서 presigned URL 생성
        // return assetService.getPresignedUrl(job.getResultAssetId());
        return null;
    }
}
