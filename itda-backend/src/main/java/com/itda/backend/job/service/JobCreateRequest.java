package com.itda.backend.job.service;

import com.itda.backend.job.domain.JobType;

/**
 * Job 생성 요청 파라미터 묶음
 */
public record JobCreateRequest(
        JobType type,
        Long projectId,
        Long sceneId,
        Long nodeId,
        String requestJson,
        String idempotencyKey
) {
}
