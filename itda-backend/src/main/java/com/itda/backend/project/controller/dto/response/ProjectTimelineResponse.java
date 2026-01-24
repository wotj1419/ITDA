package com.itda.backend.project.controller.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 프로젝트 타임라인 응답
 */
@Schema(description = "Project timeline response")
public record ProjectTimelineResponse(
        @Schema(description = "Timeline items")
        List<ProjectTimelineItem> items
) {
}
