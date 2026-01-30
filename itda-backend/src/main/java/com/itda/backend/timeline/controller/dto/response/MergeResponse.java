package com.itda.backend.timeline.controller.dto.response;

import com.itda.backend.job.domain.Job;
import com.itda.backend.job.domain.JobStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Merge job response")
public record MergeResponse(

        @Schema(description = "Job ID (null if cached)", example = "1001") Long jobId,

        @Schema(description = "Job status", example = "PENDING") JobStatus status,

        @Schema(description = "Whether result was served from cache", example = "false") boolean cached) {

    public static MergeResponse from(Job job) {
        return new MergeResponse(job.getId(), job.getStatus(), false);
    }

    public static MergeResponse cacheHit() {
        return new MergeResponse(null, JobStatus.SUCCEEDED, true);
    }
}
