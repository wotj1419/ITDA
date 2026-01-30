package com.itda.backend.scene.controller.dto.response;

import com.itda.backend.scene.repository.dto.SceneSummary;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Scene summary response")
public record SceneSummaryResponse(

        @Schema(description = "Scene ID", example = "201")
        Long sceneId,

        @Schema(description = "Scene title", example = "Scene 1: Mars Base")
        String title,

        @Schema(description = "Scene description", example = "The crew arrives at the Mars base.")
        String description,

        @Schema(description = "Order index", example = "1")
        Integer order,

        @Schema(description = "Scene status", example = "DRAFT")
        String status
) {
    public static SceneSummaryResponse from(SceneSummary summary) {
        return new SceneSummaryResponse(
                summary.getSceneId(),
                summary.getTitle(),
                summary.getDescription(),
                summary.getOrderIndex(),
                null
        );
    }
}
