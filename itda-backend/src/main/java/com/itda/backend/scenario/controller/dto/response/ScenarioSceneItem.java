package com.itda.backend.scenario.controller.dto.response;

import com.itda.backend.scene.domain.Scene;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Scenario scene item")
public record ScenarioSceneItem(

        @Schema(description = "Scene ID", example = "201")
        Long sceneId,

        @Schema(description = "Scene order", example = "1")
        Integer order,

        @Schema(description = "Scene title", example = "고립된 아침")
        String title,

        @Schema(description = "Scene description")
        String description
) {

    public static ScenarioSceneItem from(Scene scene) {
        return new ScenarioSceneItem(
                scene.getId(),
                scene.getOrderIndex(),
                scene.getTitle(),
                scene.getDescription()
        );
    }
}
