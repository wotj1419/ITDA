package com.itda.backend.project.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Project update request")
public record UpdateProjectRequest(

        @Schema(description = "Project title", example = "Updated Title")
        @NotBlank(message = "title must not be blank")
        @Size(max = 200, message = "title must be 200 characters or less")
        String title,

        @Schema(description = "Project description", example = "Updated description")
        @NotNull(message = "description is required")
        @Size(max = 2000, message = "description must be 2000 characters or less")
        String description,

        @Schema(description = "Project genre", example = "COMEDY")
        @NotBlank(message = "genre must not be blank")
        @Size(max = 100, message = "genre must be 100 characters or less")
        String genre
) {
}
