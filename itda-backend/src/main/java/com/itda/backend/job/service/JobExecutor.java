package com.itda.backend.job.service;

import com.itda.backend.global.config.FileStorageProperties;
import com.itda.backend.job.domain.Job;
import com.itda.backend.job.domain.JobStatus;
import com.itda.backend.job.domain.JobType;
import com.itda.backend.job.repository.JobMapper;
import com.itda.backend.node.domain.NodeStatus;
import com.itda.backend.node.repository.NodeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

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
    private final FileStorageProperties fileStorageProperties;

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
        boolean started = markRunning(jobId, maxRetryCount);
        
        if (!started) {
            log.info("[JobExecutor] Job not eligible to run (status changed), skipping: id={}", jobId);
            return;
        }

        log.info("[JobExecutor] Job started: id={}, type={}", jobId, job.getType());

        try {
            updateNodeStatusIfApplicable(job, NodeStatus.RUNNING, null);
            ExecutionResult result = executeByType(job);
            if (result == null) {
                throw new IllegalStateException("Worker returned null result");
            }
            if (requiresResultAssetId(job) && result.resultAssetId() == null) {
                throw new IllegalStateException("Worker returned null resultAssetId");
            }

            // 성공 처리
            boolean succeeded = markSucceeded(jobId, result.resultAssetId());
            if (!succeeded) {
                log.warn("[JobExecutor] Job success ignored (status changed): id={}", jobId);
                return;
            }

            updateNodeStatusIfApplicable(job, NodeStatus.SUCCEEDED, result.nodeContentKey());
            log.info("[JobExecutor] Job succeeded: id={}, resultAssetId={}", jobId, result.resultAssetId());
            publishDoneSafely(jobId);

        } catch (Exception e) {
            handleFailure(job, e);
        }
    }

    /**
     * 타입별 Worker 호출
     */
    private ExecutionResult executeByType(Job job) {
        simulateDelay();
        switch (job.getType()) {
            case IMAGE_GENERATION -> {
                return executeImageGeneration(job);
            }
            case VIDEO_GENERATION -> {
                return executeVideoGeneration(job);
            }
            case SCENE_MERGE, PROJECT_MERGE -> {
                return executeProjectMerge(job);
            }
        }
        throw new IllegalStateException("Unsupported job type: " + job.getType());
    }

    /**
     * 실패 처리
     */
    private void handleFailure(Job job, Exception e) {
        Long jobId = job.getId();
        String errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        log.error("[JobExecutor] Job failed: id={}, error={}", jobId, errorMessage, e);

        boolean failed = markFailed(jobId, truncateErrorMessage(errorMessage));
        if (!failed) {
            log.warn("[JobExecutor] Job failure ignored (status changed): id={}", jobId);
            return;
        }

        updateNodeStatusIfApplicable(job, NodeStatus.FAILED, null);
        publishFailedSafely(jobId);
    }

    private void simulateDelay() {
        int delayMs = ThreadLocalRandom.current().nextInt(1000, 2001);
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Job execution interrupted", e);
        }
    }

    private ExecutionResult executeImageGeneration(Job job) {
        Long nodeId = requireNodeId(job);
        String relativePath = "ai/images/node-" + nodeId + ".png";
        String contentKey = createMockAsset(relativePath, MockAssetType.PNG);
        return new ExecutionResult(nodeId, contentKey);
    }

    private ExecutionResult executeVideoGeneration(Job job) {
        Long nodeId = requireNodeId(job);
        String relativePath = "ai/videos/node-" + nodeId + ".mp4";
        String contentKey = createMockAsset(relativePath, MockAssetType.MP4);
        return new ExecutionResult(nodeId, contentKey);
    }

    private ExecutionResult executeProjectMerge(Job job) {
        Long projectId = job.getProjectId();
        if (projectId == null) {
            throw new IllegalStateException("Project merge job missing projectId");
        }
        String relativePath = "exports/" + projectId + "/final.mp4";
        createMockAsset(relativePath, MockAssetType.MP4);
        return new ExecutionResult(null, null);
    }

    private Long requireNodeId(Job job) {
        if (job.getNodeId() == null) {
            throw new IllegalStateException("Node job missing nodeId");
        }
        return job.getNodeId();
    }

    private void updateNodeStatusIfApplicable(Job job, NodeStatus status, String contentUrl) {
        if (job.getNodeId() == null) {
            return;
        }
        if (job.getType() != JobType.IMAGE_GENERATION
                && job.getType() != JobType.VIDEO_GENERATION) {
            return;
        }
        int updated;
        if (contentUrl == null) {
            updated = nodeMapper.updateStatus(job.getNodeId(), status);
        } else {
            updated = nodeMapper.updateStatusAndContentUrl(job.getNodeId(), status, contentUrl);
        }
        if (updated == 0) {
            log.warn("[JobExecutor] Node status update ignored: jobId={}, nodeId={}, status={}, contentUrlPresent={}",
                    job.getId(), job.getNodeId(), status, contentUrl != null);
        }
    }

    private String createMockAsset(String relativePath, MockAssetType assetType) {
        Path targetPath = resolveUploadPath(relativePath);
        try {
            Files.createDirectories(targetPath.getParent());
            if (!Files.exists(targetPath)) {
                Files.write(targetPath, assetType.bytes());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create mock asset", e);
        }
        return normalizeRelativePath(relativePath);
    }

    private Path resolveUploadPath(String relativePath) {
        return Path.of(fileStorageProperties.getUploadDir()).resolve(relativePath);
    }

    private String normalizeRelativePath(String relativePath) {
        return relativePath.replace("\\", "/");
    }

    private record ExecutionResult(Long resultAssetId, String nodeContentKey) {
    }

    private enum MockAssetType {
        PNG(new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
                0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4,
                (byte) 0x89, 0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41,
                0x54, 0x78, (byte) 0x9C, 0x63, 0x00, 0x01, 0x00, 0x00,
                0x05, 0x00, 0x01, 0x0D, 0x0A, 0x2D, (byte) 0xB4, 0x00,
                0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, (byte) 0xAE,
                0x42, 0x60, (byte) 0x82
        }),
        MP4("ITDA MOCK VIDEO".getBytes());

        private final byte[] bytes;

        MockAssetType(byte[] bytes) {
            this.bytes = bytes;
        }

        public byte[] bytes() {
            return bytes;
        }
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
        return job.getType() == JobType.IMAGE_GENERATION
                || job.getType() == JobType.VIDEO_GENERATION;
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
