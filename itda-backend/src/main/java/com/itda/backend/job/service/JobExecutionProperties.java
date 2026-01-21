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
     * <p>
     * 0 이하일 경우 제한 없음 (수동 재시도 정책을 위한 기본값)
     */
    private int maxRetryCount = 0;

    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    public void setMaxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }
}
