package com.itda.backend.node.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Node generate job request")
public record GenerateNodeJobRequest(
        @Schema(description = "Prompt override (optional)", example = "a cinematic master shot")
        String prompt,

        @Schema(description = "Idempotency key (optional)", example = "node-generate-001")
        String idempotencyKey,

        @Schema(description = "Requeue if existing job is pending/failed", example = "false")
        Boolean requeueIfExisting
) {
}
