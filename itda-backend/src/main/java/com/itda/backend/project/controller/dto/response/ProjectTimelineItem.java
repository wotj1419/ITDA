package com.itda.backend.project.controller.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 프로젝트 타임라인 항목
 */
@Schema(description = "Project timeline item")
public record ProjectTimelineItem(
        @Schema(description = "VIDEO node ID", example = "301")
        Long videoNodeId,

        @Schema(description = "Scene ID", example = "201")
        Long sceneId,

        @Schema(description = "Timeline order (1..N)", example = "1")
        Integer order,

        @Schema(description = "Content URL", example = "/api/nodes/301/content")
        String url,

        @Schema(description = "Thumbnail URL", example = "/files/ai/thumbnails/scene-1.png")
        String thumbnailUrl,

        @Schema(description = "Video URL", example = "/files/ai/videos/node-301.mp4")
        String videoUrl
) {
}
