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

    public static Scene create(Long projectId, String title, String description, int orderIndex) {
        if (projectId == null) {
            throw new IllegalArgumentException("projectId must not be null");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        if (orderIndex < 1) {
            throw new IllegalArgumentException("orderIndex must be positive");
        }

        return Scene.builder()
                .projectId(projectId)
                .title(title)
                .description(description)
                .orderIndex(orderIndex)
                .build();
    }
}
