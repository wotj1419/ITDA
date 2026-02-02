package com.itda.backend.timeline.controller.dto.response;

import com.itda.backend.timeline.repository.dto.ProjectTimelineItem;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Project timeline item response")
public record ProjectTimelineItemResponse(

        @Schema(description = "Scene video ID", example = "401") Long sceneVideoId,

        @Schema(description = "Scene ID", example = "201") Long sceneId,

        @Schema(description = "Scene title", example = "Mars Base") String sceneTitle,

        @Schema(description = "Legacy URL (video)", example = "/files/ai/videos/scene-401.mp4") String url,

        @Schema(description = "Thumbnail URL", example = "https://...") String thumbnailUrl,

        @Schema(description = "Video URL", example = "/files/ai/videos/scene-401.mp4") String videoUrl,

        @Schema(description = "Duration", example = "5") Integer duration,

        @Schema(description = "Order index", example = "1") Integer order) {
    public static ProjectTimelineItemResponse from(ProjectTimelineItem item, String url, String thumbnailUrl, String videoUrl) {
        return new ProjectTimelineItemResponse(
                item.getSceneVideoId(),
                item.getSceneId(),
                item.getSceneTitle(),
                url,
                thumbnailUrl,
                videoUrl,
                item.getDuration(),
                item.getOrderIndex());
    }
}
