package com.itda.backend.project.controller.dto.response;

import com.itda.backend.project.repository.dto.ProjectSummary;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Project summary response")
public record ProjectSummaryResponse(

        @Schema(description = "Project ID", example = "101")
        Long projectId,

        @Schema(description = "Project title", example = "Mars Vlog")
        String title,

        @Schema(description = "Thumbnail URL", example = "https://...")
        String thumbnailUrl,

        @Schema(description = "My role in project", example = "OWNER")
        String role,

        @Schema(description = "Member count", example = "3")
        Integer memberCount,

        @Schema(description = "Scene count", example = "5")
        Integer sceneCount,

        @Schema(description = "Updated at", example = "2026-01-15T12:00:00")
        LocalDateTime updatedAt
) {
    public static ProjectSummaryResponse from(ProjectSummary summary) {
        return new ProjectSummaryResponse(
                summary.getProjectId(),
                summary.getTitle(),
                null,
                summary.getMyRole(),
                summary.getMemberCount(),
                summary.getSceneCount(),
                summary.getUpdatedAt()
        );
    }
}
