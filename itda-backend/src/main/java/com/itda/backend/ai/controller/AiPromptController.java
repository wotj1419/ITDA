package com.itda.backend.ai.controller;

import com.itda.backend.ai.controller.dto.request.AiPromptGenerateRequest;
import com.itda.backend.ai.controller.dto.request.AiPromptImproveRequest;
import com.itda.backend.ai.controller.dto.request.AiPromptRewriteRequest;
import com.itda.backend.ai.controller.dto.request.AiPromptTranslateRequest;
import com.itda.backend.ai.controller.dto.response.AiPromptResponse;
import com.itda.backend.ai.service.AiPromptService;
import com.itda.backend.ai.service.PromptTranslationService;
import com.itda.backend.global.response.ApiResponse;
import com.itda.backend.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class AiPromptController {

    private final AiPromptService aiPromptService;
    private final PromptTranslationService promptTranslationService;

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
        AiPromptResponse response = aiPromptService.generatePrompt(request);
        return ApiResponse.success(response);
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
        AiPromptResponse response = aiPromptService.improvePrompt(request);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "프롬프트 번역 (EN -> KO)",
            description = "영어 프롬프트를 한국어로 번역합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "번역 성공",
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
    @PostMapping("/translate")
    public ResponseEntity<ApiResponse<AiPromptResponse>> translate(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AiPromptTranslateRequest request) {
        String promptEnBase = request.promptEn();
        String promptKo;
        try {
            promptKo = promptTranslationService.translateEnToKo(promptEnBase);
        } catch (Exception e) {
            log.warn("Prompt translate failed: reason={}", e.getMessage());
            promptKo = "";
        }
        return ApiResponse.success(new AiPromptResponse(promptEnBase, promptKo));
    }

    @Operation(
            summary = "프롬프트 리라이트 (KO -> EN)",
            description = "한국어 프롬프트를 영어 원본으로 리라이트합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "리라이트 성공",
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
    @PostMapping("/rewrite")
    public ResponseEntity<ApiResponse<AiPromptResponse>> rewrite(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AiPromptRewriteRequest request) {
        String promptKo = request.promptKo();
        String promptEnBase;
        try {
            promptEnBase = promptTranslationService.rewriteKoToEn(promptKo);
        } catch (Exception e) {
            log.warn("Prompt rewrite failed: reason={}", e.getMessage());
            promptEnBase = "";
        }
        return ApiResponse.success(new AiPromptResponse(promptEnBase, promptKo));
    }
}
