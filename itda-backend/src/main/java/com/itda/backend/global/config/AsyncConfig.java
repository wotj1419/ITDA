package com.itda.backend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 비동기 처리 설정
 * <p>
 * LocalAsyncJobDispatcher에서 사용하는 스레드풀 정의.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Job 실행용 스레드풀
     * <p>
     * - corePoolSize: 기본 스레드 수
     * - maxPoolSize: 최대 스레드 수
     * - queueCapacity: 대기 큐 크기
     * <p>
     * 값은 서버 리소스와 예상 부하에 따라 조정 필요.
     */
    @Bean(name = "jobExecutorPool")
    public Executor jobExecutorPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("job-executor-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
