package com.itda.backend.ai.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * EN -> KO prompt translation request
 */
@Schema(description = "Prompt translation request (EN -> KO)")
public record AiPromptTranslateRequest(
        @Schema(description = "English prompt", example = "A cinematic morning in a sunlit cafe.")
        @NotBlank
        String promptEn
) {
}
