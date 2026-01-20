package com.itda.backend.job.controller.dto;

import com.itda.backend.job.domain.Job;
import com.itda.backend.job.domain.JobStatus;
import com.itda.backend.job.domain.JobType;

import java.time.LocalDateTime;

/**
 * Job 상태 조회 응답 DTO
 *
 * @param jobId      Job ID
 * @param type       Job 타입
 * @param status     현재 상태
 * @param progress   진행률 (MVP: 항상 null)
 * @param target     대상 정보 (NODE/SCENE/PROJECT)
 * @param resultUrl  결과 presigned URL (성공 시)
 * @param error      에러 정보 (실패 시)
 * @param createdAt  생성 시각
 * @param finishedAt 완료 시각
 */
public record JobResponse(
        Long jobId,
        JobType type,
        JobStatus status,
        Integer progress,
        JobTarget target,
        String resultUrl,
        JobError error,
        LocalDateTime createdAt,
        LocalDateTime finishedAt
) {

    /**
     * Job 도메인 객체로부터 응답 DTO 생성
     *
     * @param job       Job 도메인 객체
     * @param resultUrl presigned URL (nullable)
     * @return JobResponse DTO
     */
    public static JobResponse from(Job job, String resultUrl) {
        return new JobResponse(
                job.getId(),
                job.getType(),
                job.getStatus(),
                null,  // MVP에서는 progress 미지원
                JobTarget.from(job),
                resultUrl,
                JobError.from(job),
                job.getCreatedAt(),
                job.getFinishedAt()
        );
    }
}
