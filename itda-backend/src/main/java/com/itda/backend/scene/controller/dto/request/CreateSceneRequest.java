package com.itda.backend.scene.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Scene create request")
public record CreateSceneRequest(

        @Schema(description = "Scene title", example = "Scene 1: Mars Base")
        @NotBlank(message = "title is required")
        @Size(max = 200, message = "title must be 200 characters or less")
        String title,

        @Schema(description = "Scene description", example = "Morning at the base")
        @Size(max = 2000, message = "description must be 2000 characters or less")
        String description
) {
}
