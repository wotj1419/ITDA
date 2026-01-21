package com.itda.backend.node.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 노드 위치 일괄 업데이트 요청 DTO
 */
@Schema(description = "Update node positions request")
public record UpdateNodePositionsRequest(

        @Schema(description = "Node positions list")
        @NotEmpty(message = "positions is required")
        @Valid
        List<NodePosition> positions
) {
}
