package com.itda.backend.timeline.controller.dto.response;

import com.itda.backend.timeline.repository.dto.SceneTimelineItem;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Scene timeline item response")
public record SceneTimelineItemResponse(

        @Schema(description = "Video node ID", example = "401")
        Long videoNodeId,

        @Schema(description = "Scene ID", example = "201")
        Long sceneId,

        @Schema(description = "Legacy URL (video)", example = "/files/ai/videos/node-401.mp4")
        String url,

        @Schema(description = "Thumbnail URL", example = "https://...")
        String thumbnailUrl,

        @Schema(description = "Video URL", example = "/files/ai/videos/node-401.mp4")
        String videoUrl,

        @Schema(description = "Duration", example = "5")
    Integer duration,

    @Schema(description = "Order index", example = "1")
    Integer order
) {
    public static SceneTimelineItemResponse from(SceneTimelineItem item, String url, String thumbnailUrl, String videoUrl) {
        return new SceneTimelineItemResponse(
                item.getVideoNodeId(),
                item.getSceneId(),
                url,
                thumbnailUrl,
                videoUrl,
                item.getDuration(),
                item.getOrderIndex()
        );
    }
}
