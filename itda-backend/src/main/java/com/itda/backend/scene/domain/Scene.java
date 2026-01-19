package com.itda.backend.scene.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Scene {

    private Long id;
    private Long projectId;
    private String title;
    private String description;
    private Integer orderIndex;
    private Long activeMasterNodeId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
