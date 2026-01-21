package com.itda.backend.scenario.controller.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Scenario prompt response")
public record ScenarioPromptResponse(

        @Schema(description = "Prompt info")
        ScenarioResponse.ScenarioText prompt
) {
    public static ScenarioPromptResponse of(String text, String status) {
        return new ScenarioPromptResponse(new ScenarioResponse.ScenarioText(text, status));
    }
}
