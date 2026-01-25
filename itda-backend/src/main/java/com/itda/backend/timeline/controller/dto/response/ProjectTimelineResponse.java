package com.itda.backend.timeline.controller.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Project timeline response")
public record ProjectTimelineResponse(

        @Schema(description = "Timeline items")
        List<ProjectTimelineItemResponse> items,

        @Schema(description = "Total duration", example = "13")
        Integer totalDuration
) {
}
