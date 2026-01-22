package com.itda.backend.node.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Request for confirming video node.
 */
@Schema(description = "Confirm video request")
public record ConfirmVideoRequest(
        @Schema(description = "Target video node id", example = "501")
        @NotNull(message = "nodeId is required")
        Long nodeId
) {
}
