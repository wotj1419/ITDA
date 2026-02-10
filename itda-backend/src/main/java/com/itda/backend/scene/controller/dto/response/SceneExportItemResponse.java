package com.itda.backend.scene.controller.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Scene export item response")
public record SceneExportItemResponse(
        @Schema(description = "Scene video ID", example = "401")
        Long sceneVideoId,
        @Schema(description = "Scene ID", example = "101")
        Long sceneId,
        @Schema(description = "Asset ID", example = "123")
        Long assetId,
        @Schema(description = "Preview URL", example = "https://cdn.example.com/scene.mp4")
        String previewUrl,
        @Schema(description = "Download URL", example = "/api/scenes/101/exports/401/file")
        String downloadUrl,
        @Schema(description = "Thumbnail URL", example = "https://cdn.example.com/scene-thumb.png")
        String thumbnailUrl,
        @Schema(description = "Merge signature", example = "f2c6b5...")
        String mergeSignature,
        @Schema(description = "Merge status", example = "COMPLETED")
        String status,
        @Schema(description = "Duration (ms)", example = "24000")
        Integer durationMs,
        @Schema(description = "Active export", example = "true")
        boolean active,
        @Schema(description = "Created at")
        LocalDateTime createdAt,
        @Schema(description = "Updated at")
        LocalDateTime updatedAt
) {
}
