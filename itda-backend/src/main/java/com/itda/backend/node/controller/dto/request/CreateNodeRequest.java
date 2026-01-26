package com.itda.backend.node.controller.dto.request;

import com.itda.backend.node.domain.NodeType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * 노드 생성 요청 DTO
 */
@Schema(description = "Node create request")
public record CreateNodeRequest(

        @Schema(description = "Node type (MASTER, GRID, SHOT, VIDEO)", example = "MASTER")
        @NotNull(message = "nodeType is required")
        NodeType nodeType,

        @Schema(
                description = "Parent node ID (MASTER는 null, GRID는 MASTER id, SHOT은 GRID id, VIDEO는 SHOT id)",
                example = "null",
                nullable = true
        )
        Long parentNodeId,

        @Schema(description = "AI prompt", example = "화성 기지에서의 아침 식사 풍경")
        String prompt,

        @Schema(description = "Node settings (style, ratio, etc.)")
        Map<String, Object> settings
) {
}
