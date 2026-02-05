package com.itda.backend.job.service;

import com.itda.backend.job.domain.Job;
import com.itda.backend.job.domain.JobStatus;
import com.itda.backend.job.domain.JobType;
import com.itda.backend.job.repository.JobMapper;
import com.itda.backend.node.domain.NodeStatus;
import com.itda.backend.node.repository.NodeMapper;
import com.itda.backend.timeline.service.MergeResultService;
import com.itda.backend.worker.ExecutionResult;
import com.itda.backend.worker.image.ImageGenerationWorker;
import com.itda.backend.worker.merge.MergeWorker;
import com.itda.backend.worker.video.VideoGenerationWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Arrays;
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
    private final NodeMapper nodeMapper;
    private final JobEventPublisher jobEventPublisher;
    private final TransactionTemplate transactionTemplate;
    private final JobExecutionProperties jobExecutionProperties;
    private final ImageGenerationWorker imageWorker;
    private final VideoGenerationWorker videoWorker;
    private final MergeWorker mergeWorker;
    private final MergeResultService mergeResultService;
    private final Environment environment;

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
        if (jobId == null) {
            log.warn("[JobExecutor] Skip execute: jobId is null");
            return;
        }

        Job job = findJobOrSkip(jobId);
        if (job == null) {
            return;
        }
        int maxRetryCount = jobExecutionProperties.getMaxRetryCount();

        if (shouldSkip(job, maxRetryCount)) {
            return;
        }
        if (!tryMarkRunningOrSkip(jobId, maxRetryCount)) {
            return;
        }

        log.info("[JobExecutor] Job started: id={}, type={}", jobId, job.getType());

        try {
            updateNodeStatusIfApplicable(job, NodeStatus.RUNNING, null, null, null);

            ExecutionResult result = executeByType(job);
            validateExecutionResult(job, result);

            boolean succeeded = markSucceeded(jobId, result.resultAssetId());
            if (!succeeded) {
                log.warn("[JobExecutor] Job success ignored (status changed): id={}", jobId);
                return;
            }

            if (job.getType() == JobType.SCENE_MERGE) {
                try {
                    mergeResultService.recordSceneMergeResult(
                            job,
                            result.resultAssetId(),
                            null,
                            result.thumbnailAssetId(),
                            null
                    );
                } catch (Exception e) {
                    log.error("[JobExecutor] Failed to record scene merge result: id={}", jobId, e);
                }
            } else if (job.getType() == JobType.PROJECT_MERGE) {
                try {
                    mergeResultService.recordProjectMergeResult(job, result.resultAssetId(), result.thumbnailAssetId());
                } catch (Exception e) {
                    log.error("[JobExecutor] Failed to record project merge result: id={}", jobId, e);
                }
            }

            updateNodeStatusIfApplicable(
                    job,
                    NodeStatus.SUCCEEDED,
                    result.nodeContentKey(),
                    result.resultAssetId(),
                    result.thumbnailAssetId()
            );
            log.info("[JobExecutor] Job succeeded: id={}, resultAssetId={}", jobId, result.resultAssetId());
            publishDoneSafely(jobId);

        } catch (Exception e) {
            handleFailure(job, e);
        }
    }

    private Job findJobOrSkip(Long jobId) {
        Job job = jobMapper.findById(jobId).orElse(null);
        if (job == null) {
            log.error("[JobExecutor] Job not found: id={}", jobId);
        }
        return job;
    }

    private boolean shouldSkip(Job job, int maxRetryCount) {
        if (job.isSucceeded()) {
            log.info("[JobExecutor] Job already succeeded, skipping: id={}", job.getId());
            return true;
        }

        if (!job.isExecutable(maxRetryCount)) {
            log.info("[JobExecutor] Job not executable, skipping: id={}, status={}, retryCount={}",
                    job.getId(), job.getStatus(), job.getRetryCount());
            return true;
        }

        return false;
    }

    private boolean tryMarkRunningOrSkip(Long jobId, int maxRetryCount) {
        boolean started = markRunning(jobId, maxRetryCount);
        if (!started) {
            log.info("[JobExecutor] Job not eligible to run (status changed), skipping: id={}", jobId);
        }
        return started;
    }

    /**
     * 타입별 Worker 호출
     */
    private ExecutionResult executeByType(Job job) {
        return switch (job.getType()) {
            case IMAGE_GENERATION -> executeImageGeneration(job);
            case VIDEO_GENERATION -> executeVideoGeneration(job);
            case SCENE_MERGE, PROJECT_MERGE -> executeMergeJob(job);
        };
    }

    /**
     * 실패 처리
     */
    private void handleFailure(Job job, Exception e) {
        Long jobId = job.getId();
        log.error(
                "[JobExecutor] Job failed: id={}, type={}, projectId={}, sceneId={}, nodeId={}",
                jobId,
                job.getType(),
                job.getProjectId(),
                job.getSceneId(),
                job.getNodeId(),
                e
        );
        String errorMessage = resolveUserErrorMessage(job.getType());
        if (isLocalProfile()) {
            String debugMessage = resolveDebugMessage(e);
            if (debugMessage != null && !debugMessage.isBlank()) {
                errorMessage = debugMessage;
            }
        }

        boolean failed = markFailed(jobId, truncateErrorMessage(errorMessage));
        if (!failed) {
            log.warn("[JobExecutor] Job failure ignored (status changed): id={}", jobId);
            return;
        }

        updateNodeStatusIfApplicable(job, NodeStatus.FAILED, null, null, null);
        publishFailedSafely(jobId);
    }

    private ExecutionResult executeImageGeneration(Job job) {
        requireNodeId(job);
        return imageWorker.execute(job);
    }

    private ExecutionResult executeVideoGeneration(Job job) {
        requireNodeId(job);
        return videoWorker.execute(job);
    }

    private ExecutionResult executeMergeJob(Job job) {
        return mergeWorker.execute(job);
    }

    private Long requireNodeId(Job job) {
        if (job.getNodeId() == null) {
            throw new IllegalStateException("Node job missing nodeId");
        }
        return job.getNodeId();
    }

    private void updateNodeStatusIfApplicable(
            Job job,
            NodeStatus status,
            String nodeContentKey,
            Long assetId,
            Long thumbnailAssetId
    ) {
        if (job.getNodeId() == null) {
            return;
        }
        if (job.getType() != JobType.IMAGE_GENERATION
                && job.getType() != JobType.VIDEO_GENERATION) {
            return;
        }
        int updated;
        if (nodeContentKey == null && assetId == null) {
            updated = nodeMapper.updateStatus(job.getNodeId(), status);
        } else {
            // NOTE: DB 컬럼명이 content_url 이지만, 로컬 저장소 기준으로는 storageKey가 들어갈 수 있음.
            updated = nodeMapper.updateStatusAndContentUrlAndAssetId(
                    job.getNodeId(),
                    status,
                    nodeContentKey,
                    assetId,
                    thumbnailAssetId
            );
        }
        if (updated == 0) {
            log.warn("[JobExecutor] Node status update ignored: jobId={}, nodeId={}, status={}, contentKeyPresent={}",
                    job.getId(), job.getNodeId(), status, nodeContentKey != null);
        }
    }

    /**
     * 에러 메시지 길이 제한 (DB 컬럼 사이즈 고려)
     */
    private String truncateErrorMessage(String message) {
        if (message == null) {
            return null;
        }
        if (message.length() <= MAX_ERROR_MESSAGE_LENGTH) {
            return message;
        }
        return message.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }

    private boolean markRunning(Long jobId, int maxRetryCount) {
        return Boolean.TRUE.equals(transactionTemplate.execute(status ->
                jobMapper.updateStatusIfExpected(
                        jobId,
                        List.of(JobStatus.PENDING, JobStatus.FAILED),
                        JobStatus.RUNNING,
                        null,
                        maxRetryCount
                ) > 0
        ));
    }

    private boolean requiresResultAssetId(Job job) {
        JobType type = job.getType();
        return type == JobType.IMAGE_GENERATION
                || type == JobType.VIDEO_GENERATION
                || type == JobType.SCENE_MERGE
                || type == JobType.PROJECT_MERGE;
    }

    private void validateExecutionResult(Job job, ExecutionResult result) {
        if (result == null) {
            throw new IllegalStateException("Worker returned null result");
        }
        if (requiresResultAssetId(job) && result.resultAssetId() == null) {
            throw new IllegalStateException("Worker returned null resultAssetId");
        }
        if (requiresResultAssetId(job) && isBlank(result.nodeContentKey())) {
            throw new IllegalStateException("Worker returned blank nodeContentKey");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
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

    private String resolveUserErrorMessage(JobType type) {
        if (type == null) {
            return "작업 처리에 실패했습니다. 잠시 후 다시 시도해주세요.";
        }
        return switch (type) {
            case IMAGE_GENERATION -> "이미지 생성에 실패했습니다. 잠시 후 다시 시도해주세요.";
            case VIDEO_GENERATION -> "영상 생성에 실패했습니다. 잠시 후 다시 시도해주세요.";
            case SCENE_MERGE, PROJECT_MERGE -> "병합에 실패했습니다. 잠시 후 다시 시도해주세요.";
        };
    }

    private boolean isLocalProfile() {
        String[] profiles = environment.getActiveProfiles();
        return Arrays.stream(profiles).anyMatch("local"::equalsIgnoreCase);
    }

    private String resolveDebugMessage(Exception e) {
        if (e == null) return null;
        Throwable cursor = e;
        while (cursor.getCause() != null) {
            cursor = cursor.getCause();
        }
        String message = cursor.getMessage();
        if (message == null || message.isBlank()) {
            return cursor.getClass().getSimpleName();
        }
        return message.trim();
    }
}
