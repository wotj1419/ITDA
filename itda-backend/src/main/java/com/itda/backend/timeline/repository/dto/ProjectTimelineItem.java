package com.itda.backend.timeline.repository.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProjectTimelineItem {

    private Long sceneVideoId;
    private Long sceneId;
    private String sceneTitle;
    private Long assetId;
    private String fallbackUrl;
    private Long masterAssetId;
    private String masterContentUrl;
    private Integer duration;
    private Integer orderIndex;
}
