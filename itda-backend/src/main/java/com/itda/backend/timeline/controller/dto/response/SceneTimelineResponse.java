package com.itda.backend.timeline.controller.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Scene timeline response")
public record SceneTimelineResponse(

        @Schema(description = "Timeline items")
        List<SceneTimelineItemResponse> items,

        @Schema(description = "Total duration", example = "13")
        Integer totalDuration
) {
}
