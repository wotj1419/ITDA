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
    private String thumbnailUrl;
    private Integer duration;
    private Integer orderIndex;
}
