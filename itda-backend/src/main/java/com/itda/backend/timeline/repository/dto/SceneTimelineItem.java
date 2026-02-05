package com.itda.backend.timeline.repository.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SceneTimelineItem {

    private Long videoNodeId;
    private Long sceneId;
    private Long assetId;
    private Long thumbnailAssetId;
    private String fallbackUrl;
    private Long shotAssetId;
    private String shotContentUrl;
    private Long masterAssetId;
    private String masterContentUrl;
    private Integer duration;
    private Integer orderIndex;
}
