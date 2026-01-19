package com.itda.backend.project.controller.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Project list response")
public record ProjectListResponse(

        @Schema(description = "Project items")
        List<ProjectSummaryResponse> items,

        @Schema(description = "Page index", example = "0")
        int page,

        @Schema(description = "Page size", example = "20")
        int size,

        @Schema(description = "Total count", example = "1")
        int total
) {
}
