package com.itda.backend.job.controller;

import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ApiResponse;
import com.itda.backend.global.response.ErrorCode;
import com.itda.backend.global.security.CustomUserDetails;
import com.itda.backend.job.controller.dto.JobResponse;
import com.itda.backend.job.domain.Job;
import com.itda.backend.job.service.JobResultResolver;
import com.itda.backend.job.service.JobService;
import com.itda.backend.project.service.ProjectAccessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI Job 상태 조회 API
 * <p>
 * Job 생성은 각 도메인 API(Node, Scene 등)에서 수행하고,
 * 이 컨트롤러는 상태 조회만 담당.
 */
@Tag(name = "AI 작업", description = "AI 작업(Job) 상태 조회/재시도 API")
@RestController
@RequestMapping("/api/ai/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final JobResultResolver jobResultResolver;
    private final ProjectAccessService projectAccessService;

    @Operation(
            summary = "Job 상태 조회",
            description = "Job ID로 AI 생성 작업의 현재 상태를 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = JobResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 값이 올바르지 않음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "프로젝트 접근 권한 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Job을 찾을 수 없음"
            )
    })
    @GetMapping("/{jobId}")
    public ResponseEntity<ApiResponse<JobResponse>> getJob(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "Job ID") @PathVariable Long jobId
    ) {
        Job job = jobService.getJob(jobId);
        ensureAccessible(userDetails, job);
        String resultUrl = jobResultResolver.resolve(job);
        return ApiResponse.success(JobResponse.from(job, resultUrl));
    }

    @Operation(
            summary = "Job 재큐잉",
            description = "PENDING/FAILED 상태의 Job을 재큐잉합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "재큐잉 요청 처리 성공",
                    content = @Content(schema = @Schema(implementation = JobResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 값이 올바르지 않음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "프로젝트 접근 권한 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Job을 찾을 수 없음"
            )
    })
    @PostMapping("/{jobId}/requeue")
    public ResponseEntity<ApiResponse<JobResponse>> requeueJob(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "Job ID") @PathVariable Long jobId
    ) {
        Job existingJob = jobService.getJob(jobId);
        ensureAccessible(userDetails, existingJob);

        Job requeuedJob = jobService.requeueIfExecutable(jobId);
        String resultUrl = jobResultResolver.resolve(requeuedJob);
        return ApiResponse.success(JobResponse.from(requeuedJob, resultUrl));
    }

    private void ensureAccessible(CustomUserDetails userDetails, Job job) {
        if (userDetails == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (job == null || job.getProjectId() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        projectAccessService.ensureProjectAccessible(job.getProjectId(), userDetails.getUserId());
    }
}
