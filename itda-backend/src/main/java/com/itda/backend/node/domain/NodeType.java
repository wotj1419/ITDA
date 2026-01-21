package com.itda.backend.node.domain;

/**
 * 노드 타입 enum
 * SCENE_HEADER는 가상 노드로 DB에 저장되지 않음
 */
public enum NodeType {
    SCENE_HEADER,  // 가상 노드 - 씬 정보 기반 합성
    MASTER,        // 마스터 이미지 노드
    GRID,          // 그리드 노드
    SHOT,          // 샷 노드
    VIDEO          // 영상 노드
}
