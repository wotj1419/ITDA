package com.itda.backend.node.controller.dto.response;

import com.itda.backend.node.domain.Node;
import com.itda.backend.node.domain.NodeStatus;
import com.itda.backend.node.domain.NodeType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * 노드 상세 응답 DTO
 */
@Schema(description = "Node detail response")
public record NodeDetailResponse(

        @Schema(description = "Node ID", example = "301")
        Long nodeId,

        @Schema(description = "Scene ID", example = "201")
        Long sceneId,

        @Schema(description = "Node type", example = "MASTER")
        NodeType type,

        @Schema(description = "Parent node ID", example = "300")
        Long parentNodeId,

        @Schema(description = "AI prompt", example = "화성 기지에서의 아침 식사 풍경")
        String prompt,

        @Schema(description = "Node settings")
        Map<String, Object> settings,

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

        @Schema(description = "Position X", example = "100.5")
        Float positionX,

        @Schema(description = "Position Y", example = "200.5")
        Float positionY
) {
    public static NodeDetailResponse from(
            Node node,
            Map<String, Object> settings,
            String contentUrl,
            String thumbnailUrl
    ) {
        return new NodeDetailResponse(
                node.getId(),
                node.getSceneId(),
                node.getNodeType(),
                node.getParentNodeId(),
                node.getPrompt(),
                settings,
                node.getStatus(),
                node.getIsActive(),
                node.getIsConfirmed(),
                contentUrl,
                thumbnailUrl,
                node.getPositionX(),
                node.getPositionY()
        );
    }
}
