package com.itda.backend.job.service;

import com.itda.backend.job.domain.Job;
import com.itda.backend.job.domain.JobStatus;
import com.itda.backend.job.repository.JobMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * Job 실행 진입점
 * <p>
 * Dispatcher로부터 Job ID를 받아 실제 Worker를 호출하고 결과를 처리.
 * 각 Worker(Image, Video, Merge)는 이 클래스를 통해 호출됨.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobExecutor {

    private static final int MAX_ERROR_MESSAGE_LENGTH = 2000;

    private final JobMapper jobMapper;
    private final JobEventPublisher jobEventPublisher;
    private final TransactionTemplate transactionTemplate;
    private final JobExecutionProperties jobExecutionProperties;

    // TODO: 팀원들이 Worker 구현 후 주입
    // private final ImageGenerationWorker imageWorker;   // 이용호
    // private final VideoGenerationWorker videoWorker;   // 김은서
    // private final MergeWorker mergeWorker;             // 장현준

    /**
     * Job 실행
     * <p>
     * 1. Job 조회 및 실행 가능 상태 확인
     * 2. 낙관적 락으로 RUNNING 전환
     * 3. 타입별 Worker 호출
     * 4. 성공/실패 처리 및 이벤트 발행
     *
     * @param jobId 실행할 Job ID
     */
    public void execute(Long jobId) {
        Job job = jobMapper.findById(jobId).orElse(null);
        if (job == null) {
            log.error("[JobExecutor] Job not found: id={}", jobId);
            return;
        }

        // 이미 완료된 경우 스킵 (중복 실행 방지)
        if (job.isSucceeded()) {
            log.info("[JobExecutor] Job already succeeded, skipping: id={}", jobId);
            return;
        }

        // 실행 가능 상태 확인 (PENDING 또는 재시도 가능한 FAILED)
        int maxRetryCount = jobExecutionProperties.getMaxRetryCount();
        if (!job.isExecutable(maxRetryCount)) {
            log.info("[JobExecutor] Job not executable, skipping: id={}, status={}, retryCount={}",
                    jobId, job.getStatus(), job.getRetryCount());
            return;
        }

        // RUNNING 전환 (낙관적 락 - 기대 상태 체크)
        boolean started = markRunning(jobId);
        
        if (!started) {
            log.info("[JobExecutor] Job not eligible to run (status changed), skipping: id={}", jobId);
            return;
        }

        log.info("[JobExecutor] Job started: id={}, type={}", jobId, job.getType());

        try {
            Long resultAssetId = executeByType(job);

            if (resultAssetId == null) {
                throw new IllegalStateException("Worker returned null resultAssetId");
            }

            // 성공 처리
            boolean succeeded = markSucceeded(jobId, resultAssetId);
            if (!succeeded) {
                log.warn("[JobExecutor] Job success ignored (status changed): id={}", jobId);
                return;
            }

            log.info("[JobExecutor] Job succeeded: id={}, resultAssetId={}", jobId, resultAssetId);
            publishDoneSafely(jobId);

        } catch (Exception e) {
            handleFailure(jobId, e);
        }
    }

    /**
     * 타입별 Worker 호출
     */
    private Long executeByType(Job job) {
        return switch (job.getType()) {
            case IMAGE_GENERATION -> {
                // TODO: 이용호 구현 후 주석 해제
                // yield imageWorker.execute(job);
                throw new UnsupportedOperationException("IMAGE_GENERATION worker not implemented");
            }
            case VIDEO_GENERATION -> {
                // TODO: 김은서 구현 후 주석 해제
                // yield videoWorker.execute(job);
                throw new UnsupportedOperationException("VIDEO_GENERATION worker not implemented");
            }
            case SCENE_MERGE, PROJECT_MERGE -> {
                // TODO: 장현준 구현 후 주석 해제
                // yield mergeWorker.execute(job);
                throw new UnsupportedOperationException("MERGE worker not implemented");
            }
        };
    }

    /**
     * 실패 처리
     */
    private void handleFailure(Long jobId, Exception e) {
        String errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        log.error("[JobExecutor] Job failed: id={}, error={}", jobId, errorMessage, e);

        boolean failed = markFailed(jobId, truncateErrorMessage(errorMessage));
        if (!failed) {
            log.warn("[JobExecutor] Job failure ignored (status changed): id={}", jobId);
            return;
        }

        publishFailedSafely(jobId);
    }

    /**
     * 에러 메시지 길이 제한 (DB 컬럼 사이즈 고려)
     */
    private String truncateErrorMessage(String message) {
        if (message == null) return null;
        return message.length() > MAX_ERROR_MESSAGE_LENGTH
                ? message.substring(0, MAX_ERROR_MESSAGE_LENGTH)
                : message;
    }

    private boolean markRunning(Long jobId) {
        return Boolean.TRUE.equals(transactionTemplate.execute(status ->
                jobMapper.updateStatusIfExpected(
                        jobId,
                        List.of(JobStatus.PENDING, JobStatus.FAILED),
                        JobStatus.RUNNING,
                        null
                ) > 0
        ));
    }

    private boolean markSucceeded(Long jobId, Long resultAssetId) {
        return Boolean.TRUE.equals(transactionTemplate.execute(status ->
                jobMapper.updateResultIfRunning(jobId, resultAssetId) > 0
        ));
    }

    private boolean markFailed(Long jobId, String errorMessage) {
        return Boolean.TRUE.equals(transactionTemplate.execute(status ->
                jobMapper.updateFailureIfRunning(jobId, errorMessage) > 0
        ));
    }

    private void publishDoneSafely(Long jobId) {
        try {
            jobEventPublisher.publishDone(jobId);
        } catch (Exception e) {
            log.warn("[JobExecutor] Failed to publish done event: id={}", jobId, e);
        }
    }

    private void publishFailedSafely(Long jobId) {
        try {
            jobEventPublisher.publishFailed(jobId);
        } catch (Exception e) {
            log.warn("[JobExecutor] Failed to publish failed event: id={}", jobId, e);
        }
    }
}
