package com.itda.backend.job.repository;

import com.itda.backend.job.domain.Job;
import com.itda.backend.job.domain.JobStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * Job(generation_jobs) 테이블 MyBatis Mapper
 */
@Mapper
public interface JobMapper {

    /**
     * Job 저장 (ID 자동 생성)
     */
    void insert(Job job);

    /**
     * ID로 Job 조회
     */
    Optional<Job> findById(@Param("id") Long id);

    /**
     * Idempotency Key로 Job 조회 (중복 요청 확인용)
     */
    Optional<Job> findByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    /**
     * 프로젝트 ID로 Job 목록 조회 (최신순)
     */
    List<Job> findByProjectId(@Param("projectId") Long projectId);

    /**
     * 상태별 Job 목록 조회 (오래된순 - FIFO)
     */
    List<Job> findByStatus(@Param("status") JobStatus status);

    /**
     * 상태 조건부 업데이트 (낙관적 락)
     * <p>
     * 현재 상태가 expectedStatuses 중 하나일 때만 새 상태로 변경.
     * 동시 실행 방지를 위한 핵심 메서드.
     *
     * @param id               Job ID
     * @param expectedStatuses 기대하는 현재 상태 목록
     * @param newStatus        변경할 상태
     * @param errorMessage     에러 메시지 (FAILED 시)
     * @return 업데이트된 행 수 (0이면 조건 불일치)
     */
    int updateStatusIfExpected(@Param("id") Long id,
                               @Param("expectedStatuses") List<JobStatus> expectedStatuses,
                               @Param("newStatus") JobStatus newStatus,
                               @Param("errorMessage") String errorMessage);

    /**
     * 결과 저장 및 성공 처리 (RUNNING 상태에서만)
     */
    int updateResultIfRunning(@Param("id") Long id,
                              @Param("resultAssetId") Long resultAssetId);

    /**
     * 실패 처리 (RUNNING 상태에서만, 재시도 횟수 증가 포함)
     */
    int updateFailureIfRunning(@Param("id") Long id,
                               @Param("errorMessage") String errorMessage);
}
