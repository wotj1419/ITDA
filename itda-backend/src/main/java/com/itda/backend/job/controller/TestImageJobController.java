package com.itda.backend.job.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ApiResponse;
import com.itda.backend.global.response.ErrorCode;
import com.itda.backend.job.controller.dto.JobResponse;
import com.itda.backend.job.controller.dto.request.TestImageJobRequest;
import com.itda.backend.job.domain.Job;
import com.itda.backend.job.domain.JobType;
import com.itda.backend.job.service.JobResultResolver;
import com.itda.backend.job.service.JobService;
import com.itda.backend.node.repository.NodeMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Test Jobs", description = "테스트용 이미지 Job 생성")
@Profile({"local", "test"})
@RestController
@RequestMapping("/test/ai")
@RequiredArgsConstructor
public class TestImageJobController {

    private final JobService jobService;
    private final JobResultResolver jobResultResolver;
    private final ObjectMapper objectMapper;
    private final NodeMapper nodeMapper;

    @Operation(summary = "테스트 이미지 Job 생성", description = "IMAGE_GENERATION Job을 생성하고 즉시 디스패치합니다.")
    @PostMapping("/image-jobs")
    public ResponseEntity<ApiResponse<JobResponse>> createImageJob(
            @Valid @RequestBody TestImageJobRequest request
    ) {
        String requestJson = toRequestJson(request.prompt());
        boolean requeueIfExisting = Boolean.TRUE.equals(request.requeueIfExisting());
        Long nodeId = request.nodeId();
        if (nodeId != null) {
            nodeMapper.findById(nodeId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NODE_NOT_FOUND));
        }

        Job job = jobService.createAndEnqueue(
                JobType.IMAGE_GENERATION,
                request.projectId(),
                null,
                nodeId,
                requestJson,
                request.idempotencyKey(),
                requeueIfExisting
        );

        String resultUrl = jobResultResolver.resolve(job);
        return ApiResponse.success(JobResponse.from(job, resultUrl));
    }

    private String toRequestJson(String prompt) {
        try {
            return objectMapper.writeValueAsString(Map.of("prompt", prompt));
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, e);
        }
    }
}
