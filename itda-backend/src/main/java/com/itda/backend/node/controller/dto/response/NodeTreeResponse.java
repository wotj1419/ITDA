package com.itda.backend.node.controller.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 씬 노드 트리 응답 DTO
 * scene_header 노드를 포함한 flat 리스트
 */
@Schema(description = "Scene node tree response")
public record NodeTreeResponse(

        @Schema(description = "Nodes in the scene (including SCENE_HEADER)")
        List<NodeSummaryResponse> nodes
) {
}
