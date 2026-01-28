package com.itda.backend.job.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI 생성 작업 도메인 모델
 * <p>
 * DB 테이블: generation_jobs
 * 불변성을 유지하되, 상태 전이 메서드를 통해 제어된 변경만 허용
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Job {

    private Long id;
    private Long projectId;
    private Long sceneId;
    private Long nodeId;

    private JobType type;

    /** 중복 요청 방지 키 (클라이언트 제공 또는 시스템 생성) */
    private String idempotencyKey;

    /** 병합 요청 서명 (timeline_items 기반 해시) */
    private String mergeSignature;

    /** 병합 요청 소스 (SCENE / PROJECT) */
    private MergeSource mergeSource;

    @Builder.Default
    private JobStatus status = JobStatus.PENDING;

    /** 입력 파라미터 (JSON 문자열) */
    private String requestJson;
    
    /** 결과 Asset ID (성공 시) */
    private Long resultAssetId;
    
    /** 에러 메시지 (실패 시) */
    private String errorMessage;

    @Builder.Default
    private Integer retryCount = 0;

    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    // ========== 상태 전이 메서드 ==========

    /**
     * RUNNING 상태로 전환
     */
    public void markRunning() {
        this.status = JobStatus.RUNNING;
        this.startedAt = LocalDateTime.now();
    }

    /**
     * 성공 완료 처리
     * @param resultAssetId 생성된 결과 Asset ID
     */
    public void markSucceeded(Long resultAssetId) {
        this.status = JobStatus.SUCCEEDED;
        this.resultAssetId = resultAssetId;
        this.finishedAt = LocalDateTime.now();
    }

    /**
     * 실패 처리
     * @param errorMessage 에러 메시지
     */
    public void markFailed(String errorMessage) {
        this.status = JobStatus.FAILED;
        this.errorMessage = errorMessage;
        this.finishedAt = LocalDateTime.now();
    }

    /**
     * 재시도 횟수 증가
     */
    public void incrementRetry() {
        this.retryCount++;
    }

    // ========== 상태 확인 메서드 ==========

    public boolean isSucceeded() {
        return this.status == JobStatus.SUCCEEDED;
    }

    public boolean isFailed() {
        return this.status == JobStatus.FAILED;
    }

    public boolean isRunning() {
        return this.status == JobStatus.RUNNING;
    }

    public boolean isPending() {
        return this.status == JobStatus.PENDING;
    }

    public boolean isInProgress() {
        return isPending() || isRunning();
    }

    /**
     * 재시도 가능 여부 확인
     * @param maxRetryCount 최대 재시도 횟수
     * @return 재시도 가능하면 true
     */
    public boolean canRetry(int maxRetryCount) {
        return this.status == JobStatus.FAILED && this.retryCount < maxRetryCount;
    }

    /**
     * 실행 가능 상태인지 확인 (PENDING 또는 재시도 가능한 FAILED)
     */
    public boolean isExecutable(int maxRetryCount) {
        if (isPending()) {
            return true;
        }
        if (maxRetryCount <= 0) {
            return isFailed();
        }
        return canRetry(maxRetryCount);
    }
}
