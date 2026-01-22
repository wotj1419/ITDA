package com.itda.backend.scenario.controller.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Scenario scenes generation response")
public record ScenarioScenesResponse(

        @Schema(description = "Generated scenes")
        List<ScenarioSceneItem> scenes,

        @Schema(description = "Current step", example = "SCENES")
        String currentStep
) {

    public static ScenarioScenesResponse of(List<ScenarioSceneItem> scenes, String currentStep) {
        return new ScenarioScenesResponse(scenes, currentStep);
    }
}
