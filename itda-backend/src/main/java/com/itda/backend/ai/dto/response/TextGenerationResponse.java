package com.itda.backend.ai.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "AI text generation response")
public record TextGenerationResponse(
        @Schema(description = "Generated text")
        String text
) {
}
