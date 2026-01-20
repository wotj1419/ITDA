package com.itda.backend.job.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * JobCreatedEvent 발행자
 * <p>
 * JobService에서 Job 생성 후 이 퍼블리셔를 통해 이벤트 발행.
 * TransactionalEventListener가 AFTER_COMMIT 시점에 이벤트 수신.
 */
@Component
@RequiredArgsConstructor
public class JobCreatedEventPublisher {

    private final ApplicationEventPublisher publisher;

    /**
     * Job 생성 이벤트 발행
     *
     * @param jobId 생성된 Job ID
     */
    public void publish(Long jobId) {
        publisher.publishEvent(new JobCreatedEvent(jobId));
    }
}
