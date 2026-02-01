package com.itda.backend.node.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 노드 도메인 모델
 * 씬 캔버스의 노드 (MASTER, GRID, SHOT, VIDEO)
 * SCENE_HEADER는 씬 헤더 노드로 DB에 저장됨
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

    // 캔버스 좌표
    private Float positionX;
    private Float positionY;

    // AI 생성 관련
    private String prompt;
    private String dataJson;        // settings JSON 문자열

    // 상태 관련
    @Builder.Default
    private NodeStatus status = NodeStatus.PENDING;

    @Builder.Default
    private Boolean isActive = false;      // MASTER용 활성 플래그

    @Builder.Default
    private Boolean isConfirmed = false;   // VIDEO용 확정 플래그

    private String contentUrl;      // 생성 결과 URL
    private Long assetId;           // 생성 결과 Asset ID

    // VIDEO 전용 - 시작/종료 샷 노드
    private Long startShotNodeId;
    private Long endShotNodeId;

    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
