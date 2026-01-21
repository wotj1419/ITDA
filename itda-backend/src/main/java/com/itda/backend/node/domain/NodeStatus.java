package com.itda.backend.node.domain;

/**
 * 노드 상태 enum
 * Job 실행 결과에 따라 업데이트됨
 */
public enum NodeStatus {
    PENDING,    // 대기 중 (초기 상태)
    RUNNING,    // 실행 중
    SUCCEEDED,  // 성공
    FAILED      // 실패
}
