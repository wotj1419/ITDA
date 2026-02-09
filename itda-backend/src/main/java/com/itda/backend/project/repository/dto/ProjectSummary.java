package com.itda.backend.project.repository.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ProjectSummary {

    private Long projectId;
    private String title;
    private String myRole;
    private Integer memberCount;
    private Integer sceneCount;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
