package com.itda.backend.scenario.repository.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ScenarioRecord {

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
}
