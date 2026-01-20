package com.itda.backend.job.dispatcher;

import com.itda.backend.job.domain.Job;
import com.itda.backend.job.service.JobDispatcher;
import com.itda.backend.job.service.JobExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 로컬 개발용 Job Dispatcher
 * <p>
 * Spring @Async를 사용하여 별도 스레드에서 Job 실행.
 * Redis Streams 없이도 비동기 Job 처리 테스트 가능.
 * <p>
 * 운영 환경에서는 RedisStreamsJobDispatcher로 교체 예정.
 */
@Slf4j
@Service
@Profile("!redis-streams")  // redis-streams 프로파일이 아닐 때 활성화
@RequiredArgsConstructor
public class LocalAsyncJobDispatcher implements JobDispatcher {

    private final JobExecutor jobExecutor;

    /**
     * Job을 비동기로 실행
     * <p>
     * jobExecutorPool 스레드풀에서 실행됨.
     * 호출 즉시 반환되고 실제 실행은 백그라운드에서 진행.
     *
     * @param job 실행할 Job
     */
    @Override
    @Async("jobExecutorPool")
    public void enqueue(Job job) {
        log.info("[LocalDispatcher] Job enqueued: id={}, type={}", job.getId(), job.getType());
        
        try {
            jobExecutor.execute(job.getId());
        } catch (Exception e) {
            // @Async 메서드에서 예외가 발생해도 호출자에게 전파되지 않음
            // 로깅만 수행 (실제 에러 처리는 JobExecutor 내부에서 수행)
            log.error("[LocalDispatcher] Unexpected error during job execution: id={}", 
                    job.getId(), e);
        }
    }
}
