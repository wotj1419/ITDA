package com.itda.backend.project.controller.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 프로젝트 내보내기 응답
 */
@Schema(description = "Project export response")
public record ProjectExportResponse(
        @Schema(description = "Export URL", example = "/api/projects/101/export/file")
        String exportUrl
) {
}
