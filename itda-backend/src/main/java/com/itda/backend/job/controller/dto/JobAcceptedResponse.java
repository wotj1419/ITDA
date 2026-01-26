package com.itda.backend.job.controller.dto;

import com.itda.backend.job.domain.Job;
import com.itda.backend.job.domain.JobStatus;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Job 생성 수락 응답 DTO
 */
@Schema(description = "Job accepted response")
public record JobAcceptedResponse(
        @Schema(description = "Job ID", example = "123")
        Long jobId,

        @Schema(description = "Job status", example = "PENDING")
        JobStatus status
) {
    public static JobAcceptedResponse from(Job job) {
        return new JobAcceptedResponse(job.getId(), job.getStatus());
    }
}
