package com.itda.backend.ai.controller;

import com.itda.backend.ai.controller.dto.request.AiPromptGenerateRequest;
import com.itda.backend.ai.controller.dto.request.AiPromptImproveRequest;
import com.itda.backend.ai.controller.dto.response.AiPromptResponse;
import com.itda.backend.ai.service.AiPromptService;
import com.itda.backend.global.response.ApiResponse;
import com.itda.backend.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI 프롬프트", description = "AI 프롬프트 생성/개선 API")
@RestController
@RequestMapping("/api/ai/prompts")
@RequiredArgsConstructor
public class AiPromptController {

    private final AiPromptService aiPromptService;

    @Operation(
            summary = "AI 프롬프트 생성",
            description = "노드 UI에서 사용할 프롬프트를 생성합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "생성 성공",
                    content = @Content(schema = @Schema(implementation = AiPromptResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "입력 값 검증 실패"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요"
            )
    })
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<AiPromptResponse>> generate(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AiPromptGenerateRequest request) {
        String prompt = aiPromptService.generatePrompt(request);
        return ApiResponse.success(new AiPromptResponse(prompt));
    }

    @Operation(
            summary = "AI 프롬프트 개선",
            description = "기존 프롬프트를 개선합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "개선 성공",
                    content = @Content(schema = @Schema(implementation = AiPromptResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "입력 값 검증 실패"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요"
            )
    })
    @PostMapping("/improve")
    public ResponseEntity<ApiResponse<AiPromptResponse>> improve(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AiPromptImproveRequest request) {
        String prompt = aiPromptService.improvePrompt(request);
        return ApiResponse.success(new AiPromptResponse(prompt));
    }
}
