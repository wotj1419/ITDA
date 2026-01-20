package com.itda.backend.job.controller.dto;

import com.itda.backend.job.domain.Job;

/**
 * Job 에러 정보 DTO
 *
 * @param code    에러 코드
 * @param message 에러 메시지
 */
public record JobError(
        String code,
        String message
) {

    private static final String JOB_EXECUTION_FAILED = "JOB_EXECUTION_FAILED";

    /**
     * Job 실패 시 에러 정보 추출
     *
     * @return 에러 정보 (실패하지 않았으면 null)
     */
    public static JobError from(Job job) {
        if (job.getErrorMessage() == null) {
            return null;
        }
        return new JobError(JOB_EXECUTION_FAILED, job.getErrorMessage());
    }
}
