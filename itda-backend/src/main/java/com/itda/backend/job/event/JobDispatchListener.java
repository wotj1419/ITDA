package com.itda.backend.job.event;

import com.itda.backend.job.repository.JobMapper;
import com.itda.backend.job.service.JobDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Job 생성 이벤트 리스너
 * <p>
 * 트랜잭션 커밋 후(AFTER_COMMIT)에 Job을 Dispatcher에 전달.
 * 이를 통해 DB 롤백과 큐 메시지 발행의 불일치 문제 방지.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobDispatchListener {

    private final JobMapper jobMapper;
    private final JobDispatcher jobDispatcher;

    /**
     * Job 생성 완료 후 Dispatcher에 enqueue
     * <p>
     * AFTER_COMMIT 시점에 실행되므로 DB에 Job이 확실히 존재함.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onJobCreated(JobCreatedEvent event) {
        jobMapper.findById(event.jobId()).ifPresentOrElse(
                job -> {
                    jobDispatcher.enqueue(job);
                    log.info("[JobDispatchListener] Job enqueued: id={}, type={}", 
                            job.getId(), job.getType());
                },
                () -> log.warn("[JobDispatchListener] Job not found after commit: id={}", 
                        event.jobId())
        );
    }
}
