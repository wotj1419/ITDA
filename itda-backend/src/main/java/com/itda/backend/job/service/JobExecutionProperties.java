package com.itda.backend.job.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Job 실행 관련 설정
 */
@Component
@ConfigurationProperties(prefix = "job.execution")
public class JobExecutionProperties {

    /**
     * 최대 재시도 횟수
     */
    private int maxRetryCount = 3;

    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    public void setMaxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }
}
