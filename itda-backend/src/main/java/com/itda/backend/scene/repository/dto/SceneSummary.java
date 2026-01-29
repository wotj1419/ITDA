package com.itda.backend.scene.repository.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SceneSummary {

    private Long sceneId;
    private String title;
    private String description;
    private Integer orderIndex;
}
