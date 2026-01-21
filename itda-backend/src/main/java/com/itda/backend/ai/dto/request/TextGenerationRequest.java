package com.itda.backend.ai.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "AI text generation request")
public record TextGenerationRequest(
        @Schema(description = "Prompt for text generation")
        String prompt,

        @Schema(description = "Model id override (optional)")
        String model
) {
}
