package com.itda.backend.node.controller.dto.response;

import com.itda.backend.job.domain.Job;
import com.itda.backend.job.domain.JobStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Node generate job response")
public record GenerateNodeJobResponse(
        @Schema(description = "Job ID", example = "456")
        Long jobId,

        @Schema(description = "Node ID", example = "301")
        Long nodeId,

        @Schema(description = "Job status", example = "PENDING")
        JobStatus status
) {
    public static GenerateNodeJobResponse from(Job job, Long nodeId) {
        return new GenerateNodeJobResponse(job.getId(), nodeId, job.getStatus());
    }
}
