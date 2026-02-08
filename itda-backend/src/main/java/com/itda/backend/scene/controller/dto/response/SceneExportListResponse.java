package com.itda.backend.scene.controller.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Scene export list response")
public record SceneExportListResponse(
        @Schema(description = "Export items") List<SceneExportItemResponse> items,
        @Schema(description = "Page number (1-based)") int page,
        @Schema(description = "Page size") int size,
        @Schema(description = "Total export count") int totalCount,
        @Schema(description = "Total page count") int totalPages
) {
}
