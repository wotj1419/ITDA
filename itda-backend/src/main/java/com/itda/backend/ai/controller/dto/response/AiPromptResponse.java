package com.itda.backend.ai.controller.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 프롬프트 응답 DTO
 */
@Schema(description = "AI prompt response")
public record AiPromptResponse(
        @Schema(description = "Generated prompt", example = "화성 기지에서의 아침 식사, cinematic...")
        String prompt
) {
}
