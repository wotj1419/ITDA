package com.itda.backend.job.domain;

/**
 * Job 실행 상태
 * <p>
 * 상태 전이: PENDING → RUNNING → SUCCEEDED | FAILED
 * FAILED 상태에서 재시도 시 다시 PENDING/RUNNING으로 전이 가능
 */
public enum JobStatus {
    
    /** 큐에 들어감 (아직 실행 안 함) */
    PENDING,
    
    /** Worker가 가져가 실행 중 */
    RUNNING,
    
    /** 성공적으로 완료 */
    SUCCEEDED,
    
    /** 실행 실패 */
    FAILED
}
