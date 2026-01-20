package com.itda.backend.project.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Project create request")
public record CreateProjectRequest(

        @Schema(description = "Project title", example = "Mars Vlog")
        @NotBlank(message = "title is required")
        @Size(max = 200, message = "title must be 200 characters or less")
        String title,

        @Schema(description = "Project description", example = "A story on Mars")
        @Size(max = 2000, message = "description must be 2000 characters or less")
        String description,

        @Schema(description = "Project genre", example = "SF")
        @Size(max = 100, message = "genre must be 100 characters or less")
        String genre
) {
}
