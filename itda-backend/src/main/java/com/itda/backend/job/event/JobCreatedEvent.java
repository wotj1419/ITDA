package com.itda.backend.job.event;

/**
 * Job 생성 완료 이벤트
 * <p>
 * 트랜잭션 커밋 후 Dispatcher가 Job을 큐에 넣도록 트리거하는 이벤트.
 * Spring ApplicationEventPublisher를 통해 발행됨.
 *
 * @param jobId 생성된 Job ID
 */
public record JobCreatedEvent(Long jobId) {
}
