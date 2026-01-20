package com.itda.backend.job.service;

import com.itda.backend.job.domain.Job;

/**
 * Job을 실행 큐에 넣는 역할
 * <p>
 * 구현체:
 * - LocalAsyncJobDispatcher: 로컬 개발용 (@Async 기반)
 * - RedisStreamsJobDispatcher: 운영용 (Redis Streams 기반) - 추후 구현
 */
public interface JobDispatcher {

    /**
     * Job을 실행 큐에 추가
     * <p>
     * 주의: 이 메서드는 반드시 DB 트랜잭션 커밋 후에 호출되어야 함.
     * (트랜잭션 롤백 시 큐에만 메시지가 남는 불일치 방지)
     *
     * @param job 실행할 Job (이미 DB에 저장된 상태)
     */
    void enqueue(Job job);
}
