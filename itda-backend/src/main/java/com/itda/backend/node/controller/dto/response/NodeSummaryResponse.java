package com.itda.backend.node.controller.dto.response;

import com.itda.backend.node.domain.Node;
import com.itda.backend.node.domain.NodeStatus;
import com.itda.backend.node.domain.NodeType;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 노드 요약 응답 DTO (목록용)
 */
@Schema(description = "Node summary response for list")
public record NodeSummaryResponse(

        @Schema(description = "Node ID", example = "301")
        Long nodeId,

        @Schema(description = "Node type", example = "MASTER")
        NodeType type,

        @Schema(description = "Scene title (SCENE_HEADER only)", example = "씬 1")
        String title,

        @Schema(description = "Scene description (SCENE_HEADER only)", example = "주인공이 아침을 맞이하는 장면")
        String description,

        @Schema(description = "Parent node ID", example = "300")
        Long parentNodeId,

        @Schema(description = "Node status", example = "SUCCEEDED")
        NodeStatus status,

        @Schema(description = "Is active (for MASTER)", example = "true")
        Boolean isActive,

        @Schema(description = "Is confirmed (for VIDEO)", example = "false")
        Boolean isConfirmed,

        @Schema(description = "Content URL", example = "/api/nodes/301/content")
        String contentUrl,

        @Schema(description = "Thumbnail URL", example = "https://...")
        String thumbnailUrl,

        @Schema(description = "Position")
        PositionDto position
) {
    public static NodeSummaryResponse from(Node node) {
        return from(node, node.getContentUrl(), null);
    }

    public static NodeSummaryResponse from(Node node, String contentUrl, String thumbnailUrl) {
        return new NodeSummaryResponse(
                node.getId(),
                node.getNodeType(),
                null,
                null,
                node.getParentNodeId(),
                node.getStatus(),
                node.getIsActive(),
                node.getIsConfirmed(),
                contentUrl,
                thumbnailUrl,
                new PositionDto(node.getPositionX(), node.getPositionY())
        );
    }

    @Schema(description = "Position coordinates")
    public record PositionDto(
            @Schema(description = "X coordinate") Float x,
            @Schema(description = "Y coordinate") Float y
    ) {}
}
