package com.itda.backend.scenario.controller.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Scenario plot response")
public record ScenarioPlotResponse(

        @Schema(description = "Plot info")
        ScenarioResponse.ScenarioText plot
) {
    public static ScenarioPlotResponse of(String text, String status) {
        return new ScenarioPlotResponse(new ScenarioResponse.ScenarioText(text, status));
    }
}
