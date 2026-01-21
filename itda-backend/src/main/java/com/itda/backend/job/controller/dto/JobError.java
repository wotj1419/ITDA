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
    private static final String IMAGE_GENERATION_FAILED = "IMAGE_GENERATION_FAILED";
    private static final String VIDEO_GENERATION_FAILED = "VIDEO_GENERATION_FAILED";
    private static final String MERGE_FAILED = "MERGE_FAILED";

    /**
     * Job 실패 시 에러 정보 추출
     *
     * @return 에러 정보 (실패하지 않았으면 null)
     */
    public static JobError from(Job job) {
        if (job.getErrorMessage() == null) {
            return null;
        }
        return new JobError(resolveErrorCode(job), job.getErrorMessage());
    }

    private static String resolveErrorCode(Job job) {
        return switch (job.getType()) {
            case IMAGE_GENERATION -> IMAGE_GENERATION_FAILED;
            case VIDEO_GENERATION -> VIDEO_GENERATION_FAILED;
            case SCENE_MERGE, PROJECT_MERGE -> MERGE_FAILED;
        };
    }
}
