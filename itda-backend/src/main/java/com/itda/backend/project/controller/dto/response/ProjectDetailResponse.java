package com.itda.backend.project.controller.dto.response;

import com.itda.backend.project.domain.Project;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Project detail response")
public record ProjectDetailResponse(

        @Schema(description = "Project ID", example = "101")
        Long projectId,

        @Schema(description = "Project title", example = "Mars Vlog")
        String title,

        @Schema(description = "Project description", example = "A story on Mars")
        String description,

        @Schema(description = "Project genre", example = "SF")
        String genre,

        @Schema(description = "Preview source type", example = "PROJECT_MERGE")
        String previewType,

        @Schema(description = "Preview thumbnail URL", example = "https://...")
        String previewThumbnailUrl,

        @Schema(description = "Preview video URL", example = "https://...")
        String previewVideoUrl,

        @Schema(description = "My role in project", example = "OWNER")
        String myRole,

        @Schema(description = "Owner user ID", example = "1")
        Long ownerId,

        @Schema(description = "Member count", example = "3")
        Integer memberCount,

        @Schema(description = "Created at", example = "2026-01-15T12:00:00")
        LocalDateTime createdAt
) {
    public static ProjectDetailResponse from(
            Project project,
            String role,
            Integer memberCount,
            String previewType,
            String previewThumbnailUrl,
            String previewVideoUrl
    ) {
        return new ProjectDetailResponse(
                project.getId(),
                project.getTitle(),
                project.getDescription(),
                project.getGenre(),
                previewType,
                previewThumbnailUrl,
                previewVideoUrl,
                role,
                project.getOwnerId(),
                memberCount,
                project.getCreatedAt()
        );
    }
}
