package com.itda.backend.job.service;

import com.itda.backend.job.domain.JobType;
import com.itda.backend.job.domain.MergeSource;

/**
 * Job 생성 요청 파라미터 묶음
 */
public record JobCreateRequest(
        JobType type,
        Long projectId,
        Long sceneId,
        Long nodeId,
        String requestJson,
        String idempotencyKey,
        String mergeSignature,
        MergeSource mergeSource
) {
    public JobCreateRequest(JobType type,
                            Long projectId,
                            Long sceneId,
                            Long nodeId,
                            String requestJson,
                            String idempotencyKey) {
        this(type, projectId, sceneId, nodeId, requestJson, idempotencyKey, null, null);
    }
}
