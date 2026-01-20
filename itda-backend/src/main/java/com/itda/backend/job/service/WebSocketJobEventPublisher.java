package com.itda.backend.job.service;

import com.itda.backend.job.controller.dto.JobResponse;
import com.itda.backend.job.domain.Job;
import com.itda.backend.job.event.ProjectEventWebSocketPublisher;
import com.itda.backend.job.repository.JobMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * WebSocket 기반 Job 이벤트 발행 구현체
 * <p>
 * Job 완료/실패 시 연결된 클라이언트들에게 실시간 알림 발송.
 * 현재는 로그만 출력하며, WebSocket 연동 시 실제 구현 필요.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketJobEventPublisher implements JobEventPublisher {

    private final JobMapper jobMapper;
    private final JobResultResolver jobResultResolver;
    private final ProjectEventWebSocketPublisher wsPublisher;

    @Override
    public void publishDone(Long jobId) {
        Job job = jobMapper.findById(jobId).orElse(null);
        if (job == null) {
            log.warn("[JobEvent] Job not found for done event: id={}", jobId);
            return;
        }
        if (job.getProjectId() == null) {
            log.warn("[JobEvent] Job has no projectId for done event: id={}", jobId);
            return;
        }

        String resultUrl = jobResultResolver.resolve(job);
        JobResponse response = JobResponse.from(job, resultUrl);
        wsPublisher.jobDone(job.getProjectId(), response);
        log.info("[JobEvent] job.done - jobId={}, type={}, resultUrl={}", 
                jobId, job.getType(), resultUrl);
    }

    @Override
    public void publishFailed(Long jobId) {
        Job job = jobMapper.findById(jobId).orElse(null);
        if (job == null) {
            log.warn("[JobEvent] Job not found for failed event: id={}", jobId);
            return;
        }
        if (job.getProjectId() == null) {
            log.warn("[JobEvent] Job has no projectId for failed event: id={}", jobId);
            return;
        }

        JobResponse response = JobResponse.from(job, null);
        wsPublisher.jobFailed(job.getProjectId(), response);
        log.info("[JobEvent] job.failed - jobId={}, type={}, error={}", 
                jobId, job.getType(), job.getErrorMessage());
    }
}
