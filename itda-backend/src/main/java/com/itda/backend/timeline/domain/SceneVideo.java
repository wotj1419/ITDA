package com.itda.backend.timeline.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SceneVideo {

    private Long id;
    private Long sceneId;
    private Long assetId;
    private Long thumbnailAssetId;
    private String mergeSignature;
    private String status;
    private Integer durationMs;
    private String thumbnailUrl;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
