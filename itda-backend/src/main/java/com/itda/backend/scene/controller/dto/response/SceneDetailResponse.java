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
        Integer order,

        @Schema(description = "Object IDs", example = "[1, 2, 3]")
        java.util.List<Long> objectIds
) {
    public static SceneDetailResponse from(Scene scene) {
        return from(scene, java.util.List.of());
    }

    public static SceneDetailResponse from(Scene scene, java.util.List<Long> objectIds) {
        return new SceneDetailResponse(
                scene.getId(),
                scene.getProjectId(),
                scene.getTitle(),
                scene.getDescription(),
                scene.getOrderIndex(),
                objectIds == null ? java.util.List.of() : objectIds
        );
    }
}
