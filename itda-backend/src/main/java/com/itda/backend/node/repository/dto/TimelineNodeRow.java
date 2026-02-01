package com.itda.backend.node.repository.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 프로젝트 타임라인용 확정 VIDEO 노드 조회 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TimelineNodeRow {

    private Long videoNodeId;
    private Long sceneId;
    private Integer sceneOrder;
    private Long assetId;
    private String contentUrl;
    private Long shotAssetId;
    private String shotContentUrl;
    private Long masterAssetId;
    private String masterContentUrl;
}
