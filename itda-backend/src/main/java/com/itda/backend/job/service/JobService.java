package com.itda.backend.job.service;

import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import com.itda.backend.job.domain.Job;
import com.itda.backend.job.domain.JobStatus;
import com.itda.backend.job.domain.JobType;
import com.itda.backend.job.event.JobCreatedEventPublisher;
import com.itda.backend.job.repository.JobMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * Job 비즈니스 로직 서비스
 * <p>
 * Job 생성, 조회, 상태 관리 담당.
 * Idempotency 적용으로 중복 요청 시 동일 Job 반환.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 128;

    private final JobMapper jobMapper;
    private final JobCreatedEventPublisher jobCreatedEventPublisher;
    private final JobExecutionProperties jobExecutionProperties;
    private final PlatformTransactionManager transactionManager;

    /**
     * 새 Job 생성 및 큐 전달 (Idempotency 적용)
     * <p>
     * 1. idempotencyKey가 이미 존재하면 기존 Job 반환
     * 2. 없으면 새 Job 생성 후 AFTER_COMMIT에 Dispatcher로 전달
     *
     * @param type           Job 타입
     * @param projectId      프로젝트 ID
     * @param sceneId        씬 ID (nullable)
     * @param nodeId         노드 ID (nullable)
     * @param requestJson    요청 파라미터 JSON
     * @param idempotencyKey 중복 방지 키 (nullable - 없으면 자동 생성)
     * @return 생성되거나 기존 Job
     */
    @Transactional
    public Job createAndEnqueue(JobType type,
                                Long projectId,
                                Long sceneId,
                                Long nodeId,
                                String requestJson,
                                String idempotencyKey) {
        return createAndEnqueue(
                new JobCreateRequest(type, projectId, sceneId, nodeId, requestJson, idempotencyKey),
                false
        );
    }

    /**
     * 새 Job 생성 및 큐 전달 (Idempotency 적용 + 선택적 재큐잉)
     *
     * @param requeueIfExisting 기존 Job이 PENDING/FAILED 상태면 재큐잉 여부
     */
    @Transactional
    public Job createAndEnqueue(JobType type,
                                Long projectId,
                                Long sceneId,
                                Long nodeId,
                                String requestJson,
                                String idempotencyKey,
                                boolean requeueIfExisting) {
        return createAndEnqueue(
                new JobCreateRequest(type, projectId, sceneId, nodeId, requestJson, idempotencyKey),
                requeueIfExisting
        );
    }

    /**
     * 새 Job 생성 및 큐 전달 (요청 객체 기반)
     */
    @Transactional
    public Job createAndEnqueue(JobCreateRequest request, boolean requeueIfExisting) {
        String finalKey = resolveIdempotencyKey(request);

        Job existing = jobMapper.findByIdempotencyKey(finalKey).orElse(null);
        if (existing != null) {
            boolean refreshed = maybeRequeue(existing, requeueIfExisting);
            return refreshed ? jobMapper.findById(existing.getId()).orElse(existing) : existing;
        }

        try {
            return createAndDispatch(
                    request.type(),
                    request.projectId(),
                    request.sceneId(),
                    request.nodeId(),
                    request.requestJson(),
                    finalKey
            );
        } catch (DuplicateKeyException e) {
            Job raced = findByIdempotencyKeyReadCommitted(finalKey);
            if (raced == null) {
                throw e;
            }
            boolean refreshed = maybeRequeue(raced, requeueIfExisting);
            return refreshed ? jobMapper.findById(raced.getId()).orElse(raced) : raced;
        }
    }

    /**
     * 새 Job 생성 및 큐 전달 (요청 객체 기반)
     */
    @Transactional
    public Job createAndEnqueue(JobCreateRequest request) {
        return createAndEnqueue(request, false);
    }

    /**
     * 기존 Job 재큐잉 (실행 가능 상태일 때만)
     *
     * @param jobId Job ID
     * @return Job 정보
     */
    @Transactional
    public Job requeueIfExecutable(Long jobId) {
        Job job = jobMapper.findById(jobId)
                .orElseThrow(() -> new BusinessException(ErrorCode.JOB_NOT_FOUND));
        boolean refreshed = maybeRequeue(job, true);
        return refreshed ? getJob(jobId) : job;
    }

    /**
     * Job 생성 및 이벤트 발행 (내부용)
     */
    private Job createAndDispatch(JobType type,
                                  Long projectId,
                                  Long sceneId,
                                  Long nodeId,
                                  String requestJson,
                                  String idempotencyKey) {
        Job job = Job.builder()
                .type(type)
                .projectId(projectId)
                .sceneId(sceneId)
                .nodeId(nodeId)
                .idempotencyKey(idempotencyKey)
                .requestJson(requestJson)
                .status(JobStatus.PENDING)
                .retryCount(0)
                .build();

        jobMapper.insert(job);
        log.info("[JobService] Job created: id={}, type={}, idempotencyKey={}", 
                job.getId(), type, idempotencyKey);

        // 커밋 이후에 Dispatcher가 enqueue하도록 이벤트 발행
        jobCreatedEventPublisher.publish(job.getId());

        return job;
    }

    private String resolveIdempotencyKey(JobCreateRequest request) {
        String normalized = normalizeIdempotencyKey(request.idempotencyKey());
        String finalKey = normalized != null
                ? normalized
                : JobIdempotencyKey.of(
                        request.projectId(),
                        request.type(),
                        request.nodeId(),
                        request.sceneId(),
                        request.requestJson()
                );
        validateIdempotencyKeyLength(finalKey);
        return finalKey;
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null) {
            return null;
        }
        String trimmed = idempotencyKey.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private void validateIdempotencyKeyLength(String idempotencyKey) {
        if (idempotencyKey != null && idempotencyKey.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private Job findByIdempotencyKeyReadCommitted(String idempotencyKey) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        template.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        return template.execute(status -> jobMapper.findByIdempotencyKey(idempotencyKey).orElse(null));
    }

    private boolean maybeRequeue(Job job, boolean requeueIfExisting) {
        if (!requeueIfExisting) {
            return false;
        }
        if (job.isExecutable(jobExecutionProperties.getMaxRetryCount())) {
            boolean requeued = false;
            if (job.isFailed()) {
                requeued = jobMapper.resetForRequeueIfFailed(job.getId()) > 0;
                if (!requeued) {
                    return false;
                }
            } else if (job.isPending()) {
                requeued = true;
            }
            log.info("[JobService] Requeue existing job: id={}, status={}", 
                    job.getId(), job.getStatus());
            jobCreatedEventPublisher.publish(job.getId());
            return requeued;
        }
        return false;
    }

    /**
     * Job 상태 조회
     *
     * @param jobId Job ID
     * @return Job 정보
     * @throws BusinessException JOB_NOT_FOUND
     */
    @Transactional(readOnly = true)
    public Job getJob(Long jobId) {
        return jobMapper.findById(jobId)
                .orElseThrow(() -> new BusinessException(ErrorCode.JOB_NOT_FOUND));
    }

    /**
     * 프로젝트의 모든 Job 조회 (최신순)
     *
     * @param projectId 프로젝트 ID
     * @return Job 목록
     */
    @Transactional(readOnly = true)
    public List<Job> getJobsByProject(Long projectId) {
        return jobMapper.findByProjectId(projectId);
    }

    /**
     * 특정 상태의 Job 목록 조회 (모니터링/관리용)
     *
     * @param status 조회할 상태
     * @return Job 목록
     */
    @Transactional(readOnly = true)
    public List<Job> getJobsByStatus(JobStatus status) {
        return jobMapper.findByStatus(status);
    }
}
