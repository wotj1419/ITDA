package com.itda.backend.scene.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Scene update request")
public record UpdateSceneRequest(

        @Schema(description = "Scene title", example = "Updated title")
        @Size(max = 200, message = "title must be 200 characters or less")
        String title,

        @Schema(description = "Scene description", example = "Updated description")
        @Size(max = 2000, message = "description must be 2000 characters or less")
        String description,

        @Schema(description = "Object IDs to link", example = "[1, 2, 3]")
        List<Long> objectIds
) {
}
