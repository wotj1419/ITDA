package com.itda.backend.timeline.controller.dto.response;

import com.itda.backend.timeline.repository.dto.SceneTimelineItem;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Scene timeline item response")
public record SceneTimelineItemResponse(

        @Schema(description = "Video node ID", example = "401")
        Long videoNodeId,

        @Schema(description = "Scene ID", example = "201")
        Long sceneId,

        @Schema(description = "Thumbnail URL", example = "https://...")
        String thumbnailUrl,

        @Schema(description = "Duration", example = "5")
    Integer duration,

    @Schema(description = "Order index", example = "1")
    Integer order
) {
    public static SceneTimelineItemResponse from(SceneTimelineItem item) {
        return from(item, item.getFallbackUrl());
    }

    public static SceneTimelineItemResponse from(SceneTimelineItem item, String resolvedUrl) {
        return new SceneTimelineItemResponse(
                item.getVideoNodeId(),
                item.getSceneId(),
                resolvedUrl,
                item.getDuration(),
                item.getOrderIndex()
        );
    }
}
