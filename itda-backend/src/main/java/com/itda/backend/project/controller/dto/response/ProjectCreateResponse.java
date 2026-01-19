package com.itda.backend.project.controller.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Project create response")
public record ProjectCreateResponse(

        @Schema(description = "Project ID", example = "101")
        Long projectId,

        @Schema(description = "Project title", example = "Mars Vlog")
        String title,

        @Schema(description = "My role in project", example = "OWNER")
        String role,

        @Schema(description = "Created at", example = "2026-01-15T12:00:00")
        LocalDateTime createdAt
) {
}
