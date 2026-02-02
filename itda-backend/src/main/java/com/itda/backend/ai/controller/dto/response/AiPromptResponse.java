package com.itda.backend.ai.controller.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * AI 프롬프트 응답 DTO
 */
@Schema(description = "AI prompt response")
public record AiPromptResponse(
        @Schema(description = "Generated English base prompt", example = "A calm morning breakfast inside a Mars base cafeteria.")
        String promptEnBase,

        @Schema(description = "Generated Korean prompt (translation)", example = "화성 기지 식당에서의 차분한 아침 식사 장면.")
        String promptKo,

        @Schema(description = "Timeline cuts (Korean) for GRID timeline mode", example = "[\"0-2초 구간 장면\", \"2-4초 구간 장면\"]")
        List<String> timelineCuts
) {
}
