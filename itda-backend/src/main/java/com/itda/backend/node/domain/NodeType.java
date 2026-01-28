package com.itda.backend.node.domain;

/**
 * 노드 타입 enum
 * SCENE_HEADER는 씬 헤더 노드로 DB에 저장됨
 */
public enum NodeType {
    SCENE_HEADER,  // 씬 헤더 노드
    MASTER,        // 마스터 이미지 노드
    GRID,          // 그리드 노드
    SHOT,          // 샷 노드
    VIDEO          // 영상 노드
}
