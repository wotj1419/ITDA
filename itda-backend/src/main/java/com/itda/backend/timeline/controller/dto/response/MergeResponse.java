package com.itda.backend.timeline.controller.dto.response;

import com.itda.backend.job.domain.Job;
import com.itda.backend.job.domain.JobStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Merge job response")
public record MergeResponse(

        @Schema(description = "Job ID", example = "1001")
        Long jobId,

        @Schema(description = "Job status", example = "PENDING")
        JobStatus status
) {

    public static MergeResponse from(Job job) {
        return new MergeResponse(job.getId(), job.getStatus());
    }
}
