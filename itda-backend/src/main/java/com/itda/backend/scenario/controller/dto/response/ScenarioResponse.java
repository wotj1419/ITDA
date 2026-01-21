package com.itda.backend.scenario.controller.dto.response;

import com.itda.backend.scenario.domain.ProjectScenario;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Scenario detail response")
public record ScenarioResponse(

        @Schema(description = "Project ID", example = "101")
        Long projectId,

        @Schema(description = "Scenario version", example = "1")
        Integer version,

        @Schema(description = "Current step", example = "PROMPT")
        String currentStep,

        @Schema(description = "Input info")
        ScenarioInput input,

        @Schema(description = "Prompt info")
        ScenarioText prompt,

        @Schema(description = "Plot info")
        ScenarioText plot
) {

    public static ScenarioResponse from(ProjectScenario scenario) {
        return new ScenarioResponse(
                scenario.getProjectId(),
                scenario.getVersion(),
                scenario.getCurrentStep(),
                new ScenarioInput(
                        scenario.getInputGenre(),
                        scenario.getInputMood(),
                        scenario.getInputSceneCount(),
                        scenario.getInputKeywords(),
                        scenario.getInputCharacterHints(),
                        scenario.getInputBackgroundHints(),
                        scenario.getInputReferenceStyle()
                ),
                new ScenarioText(scenario.getPromptText(), scenario.getPromptStatus()),
                new ScenarioText(scenario.getPlotText(), scenario.getPlotStatus())
        );
    }

    @Schema(description = "Scenario input")
    public record ScenarioInput(

            @Schema(description = "Genre", example = "SF")
            String genre,

            @Schema(description = "Mood", example = "HOPEFUL")
            String mood,

            @Schema(description = "Scene count", example = "5")
            Integer sceneCount,

            @Schema(description = "Keywords", example = "화성, 생존, 가족")
            String keywords,

            @Schema(description = "Character hints", example = "외로운 우주인")
            String characterHints,

            @Schema(description = "Background hints", example = "화성 기지")
            String backgroundHints,

            @Schema(description = "Reference style", example = "인터스텔라")
            String referenceStyle
    ) {
    }

    @Schema(description = "Scenario text")
    public record ScenarioText(

            @Schema(description = "Text")
            String text,

            @Schema(description = "Status", example = "DRAFT")
            String status
    ) {
    }
}
