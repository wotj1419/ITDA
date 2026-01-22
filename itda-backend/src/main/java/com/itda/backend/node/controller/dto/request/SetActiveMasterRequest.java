package com.itda.backend.node.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Request for setting active master.
 */
@Schema(description = "Set active master request")
public record SetActiveMasterRequest(
        @Schema(description = "Target master node id", example = "101")
        @NotNull(message = "nodeId is required")
        Long nodeId
) {
}
