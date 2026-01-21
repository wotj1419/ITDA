package com.itda.backend.scenario.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Scenario prompt update request")
public record UpdatePromptRequest(

        @Schema(description = "Prompt text", example = "수정된 프롬프트")
        @NotBlank(message = "text is required")
        @Size(max = 10000, message = "text must be 10000 characters or less")
        String text,

        @Schema(description = "Prompt status", example = "APPROVED")
        @NotBlank(message = "status is required")
        @Pattern(regexp = "DRAFT|APPROVED", message = "status must be DRAFT or APPROVED")
        String status
) {
}
