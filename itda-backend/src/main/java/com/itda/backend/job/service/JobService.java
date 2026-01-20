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

    private final JobMapper jobMapper;
    private final JobCreatedEventPublisher jobCreatedEventPublisher;
    private final JobExecutionProperties jobExecutionProperties;

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
        return createAndEnqueue(type, projectId, sceneId, nodeId, requestJson, idempotencyKey, false);
    }

    /**
     * 새 Job 생성 및 큐 전달 (Idempotency 적용 + 선택적 재큐잉)
     *
     * @param requeueIfExisting 기존 Job이 PENDING/재시도 가능 상태면 재큐잉 여부
     */
    @Transactional
    public Job createAndEnqueue(JobType type,
                                Long projectId,
                                Long sceneId,
                                Long nodeId,
                                String requestJson,
                                String idempotencyKey,
                                boolean requeueIfExisting) {

        String finalKey = idempotencyKey != null
                ? idempotencyKey
                : JobIdempotencyKey.of(projectId, type, nodeId, sceneId, requestJson);

        Job existing = jobMapper.findByIdempotencyKey(finalKey).orElse(null);
        if (existing != null) {
            maybeRequeue(existing, requeueIfExisting);
            return existing;
        }

        try {
            return createAndDispatch(type, projectId, sceneId, nodeId, requestJson, finalKey);
        } catch (DuplicateKeyException e) {
            Job raced = jobMapper.findByIdempotencyKey(finalKey)
                    .orElseThrow(() -> e);
            maybeRequeue(raced, requeueIfExisting);
            return raced;
        }
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
        maybeRequeue(job, true);
        return job;
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

    private void maybeRequeue(Job job, boolean requeueIfExisting) {
        if (!requeueIfExisting) {
            return;
        }
        if (job.isExecutable(jobExecutionProperties.getMaxRetryCount())) {
            log.info("[JobService] Requeue existing job: id={}, status={}", 
                    job.getId(), job.getStatus());
            jobCreatedEventPublisher.publish(job.getId());
        }
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
