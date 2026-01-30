package com.itda.backend.object.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObjectSheet {

    private Long id;
    private Long projectId;
    private String name;
    private ObjectType type;
    private String description;
    private String style;
    private String sheetImageUrl;
    private Long sheetImageAssetId;
    private ObjectStatus status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ObjectSheet create(Long projectId,
                                     String name,
                                     ObjectType type,
                                     String description,
                                     String style,
                                     Long createdBy) {
        if (projectId == null) {
            throw new IllegalArgumentException("projectId must not be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("description must not be blank");
        }

        return ObjectSheet.builder()
                .projectId(projectId)
                .name(name)
                .type(type)
                .description(description)
                .style(style)
                .status(ObjectStatus.PENDING)
                .createdBy(createdBy)
                .build();
    }
}
