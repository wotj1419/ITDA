package com.itda.backend.job.service;

/**
 * Job 완료/실패 이벤트 발행 인터페이스
 * <p>
 * API 서버가 WebSocket을 통해 클라이언트에게 Job 상태 변경을 알림.
 * LocalAsync 실행 시 JobExecutor가 직접 호출하고,
 * Redis Streams 전환 시에는 Worker 완료 콜백이 호출.
 */
public interface JobEventPublisher {

    /**
     * Job 완료 이벤트 발행 (job.done)
     *
     * @param jobId 완료된 Job ID
     */
    void publishDone(Long jobId);

    /**
     * Job 실패 이벤트 발행 (job.failed)
     *
     * @param jobId 실패한 Job ID
     */
    void publishFailed(Long jobId);
}
