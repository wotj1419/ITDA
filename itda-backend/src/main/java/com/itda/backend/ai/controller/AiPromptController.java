package com.itda.backend.ai.controller;

import com.itda.backend.ai.controller.dto.request.AiPromptGenerateRequest;
import com.itda.backend.ai.controller.dto.request.AiPromptImproveRequest;
import com.itda.backend.ai.controller.dto.response.AiPromptResponse;
import com.itda.backend.ai.service.AiPromptService;
import com.itda.backend.global.response.ApiResponse;
import com.itda.backend.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI Prompt", description = "Prompt generation APIs")
@RestController
@RequestMapping("/api/ai/prompts")
@RequiredArgsConstructor
public class AiPromptController {

    private final AiPromptService aiPromptService;

    @Operation(summary = "Generate AI prompt", description = "Generate prompt for node UI")
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<AiPromptResponse>> generate(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AiPromptGenerateRequest request) {
        String prompt = aiPromptService.generatePrompt(request);
        return ApiResponse.success(new AiPromptResponse(prompt));
    }

    @Operation(summary = "Improve AI prompt", description = "Improve existing prompt for node UI")
    @PostMapping("/improve")
    public ResponseEntity<ApiResponse<AiPromptResponse>> improve(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AiPromptImproveRequest request) {
        String prompt = aiPromptService.improvePrompt(request);
        return ApiResponse.success(new AiPromptResponse(prompt));
    }
}
