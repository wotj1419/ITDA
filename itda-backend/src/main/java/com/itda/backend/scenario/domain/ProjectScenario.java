package com.itda.backend.scenario.domain;

import com.itda.backend.scenario.repository.dto.ScenarioRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectScenario {

    private Long projectId;

    private String inputGenre;
    private String inputMood;
    private Integer inputSceneCount;
    private String inputKeywords;
    private String inputCharacterHints;
    private String inputBackgroundHints;
    private String inputReferenceStyle;

    private String promptText;
    private String promptStatus;

    private String plotText;
    private String plotStatus;

    private String currentStep;
    private Integer version;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProjectScenario fromRecord(ScenarioRecord record) {
        if (record == null) {
            return null;
        }
        return ProjectScenario.builder()
                .projectId(record.getProjectId())
                .inputGenre(record.getInputGenre())
                .inputMood(record.getInputMood())
                .inputSceneCount(record.getInputSceneCount())
                .inputKeywords(record.getInputKeywords())
                .inputCharacterHints(record.getInputCharacterHints())
                .inputBackgroundHints(record.getInputBackgroundHints())
                .inputReferenceStyle(record.getInputReferenceStyle())
                .promptText(record.getPromptText())
                .promptStatus(record.getPromptStatus())
                .plotText(record.getPlotText())
                .plotStatus(record.getPlotStatus())
                .currentStep(record.getCurrentStep())
                .version(record.getVersion())
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt())
                .build();
    }

    public ScenarioRecord toRecord() {
        ScenarioRecord record = new ScenarioRecord();
        record.setProjectId(projectId);
        record.setInputGenre(inputGenre);
        record.setInputMood(inputMood);
        record.setInputSceneCount(inputSceneCount);
        record.setInputKeywords(inputKeywords);
        record.setInputCharacterHints(inputCharacterHints);
        record.setInputBackgroundHints(inputBackgroundHints);
        record.setInputReferenceStyle(inputReferenceStyle);
        record.setPromptText(promptText);
        record.setPromptStatus(promptStatus);
        record.setPlotText(plotText);
        record.setPlotStatus(plotStatus);
        record.setCurrentStep(currentStep);
        record.setVersion(version);
        record.setCreatedAt(createdAt);
        record.setUpdatedAt(updatedAt);
        return record;
    }
}
