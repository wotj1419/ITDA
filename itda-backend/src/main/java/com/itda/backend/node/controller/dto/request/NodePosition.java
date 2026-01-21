package com.itda.backend.node.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 노드 위치 정보 DTO
 */
@Schema(description = "Node position")
public record NodePosition(

        @Schema(description = "Node ID", example = "301")
        @NotNull(message = "nodeId is required")
        Long nodeId,

        @Schema(description = "X coordinate", example = "100.5")
        @NotNull(message = "x is required")
        Float x,

        @Schema(description = "Y coordinate", example = "200.5")
        @NotNull(message = "y is required")
        Float y
) {
}
