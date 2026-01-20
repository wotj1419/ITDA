package com.itda.backend.scene.controller.dto.response;

import com.itda.backend.scene.domain.Scene;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Scene detail response")
public record SceneDetailResponse(

        @Schema(description = "Scene ID", example = "201")
        Long sceneId,

        @Schema(description = "Project ID", example = "101")
        Long projectId,

        @Schema(description = "Scene title", example = "Scene 1: Mars Base")
        String title,

        @Schema(description = "Scene description", example = "Morning at the base")
        String description,

        @Schema(description = "Order index", example = "1")
        Integer order
) {
    public static SceneDetailResponse from(Scene scene) {
        return new SceneDetailResponse(
                scene.getId(),
                scene.getProjectId(),
                scene.getTitle(),
                scene.getDescription(),
                scene.getOrderIndex()
        );
    }
}
