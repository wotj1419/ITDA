package com.itda.backend.node.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Node domain model.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Node {

    private Long id;
    private Long sceneId;
    private NodeType nodeType;
    private Long parentNodeId;
    private Integer orderIndex;

    private Float positionX;
    private Float positionY;

    private String prompt;
    private String dataJson;

    @Builder.Default
    private NodeStatus status = NodeStatus.PENDING;

    @Builder.Default
    private Boolean isActive = false;

    @Builder.Default
    private Boolean isConfirmed = false;

    private String contentUrl;
    private Long assetId;
    private Long thumbnailAssetId;

    private Long startShotNodeId;
    private Long endShotNodeId;

    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
