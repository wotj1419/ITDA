package com.itda.backend.job.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Test image job create request")
public record TestImageJobRequest(

        @Schema(description = "Project ID", example = "1")
        @NotNull(message = "projectId is required")
        Long projectId,

        @Schema(description = "Node ID (optional)", example = "10")
        Long nodeId,

        @Schema(description = "Prompt", example = "a scenic view of a mountain at sunset")
        @NotBlank(message = "prompt is required")
        String prompt,

        @Schema(description = "Idempotency key (optional)", example = "test-image-001")
        String idempotencyKey,

        @Schema(description = "Requeue if existing job is pending/failed", example = "false")
        Boolean requeueIfExisting
) {
}
