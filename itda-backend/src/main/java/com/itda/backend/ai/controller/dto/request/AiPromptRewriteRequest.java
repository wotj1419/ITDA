package com.itda.backend.ai.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * KO -> EN prompt rewrite request
 */
@Schema(description = "Prompt rewrite request (KO -> EN)")
public record AiPromptRewriteRequest(
        @Schema(description = "Korean prompt", example = "화성 기지 식당에서의 차분한 아침 식사 장면.")
        @NotBlank
        String promptKo
) {
}
