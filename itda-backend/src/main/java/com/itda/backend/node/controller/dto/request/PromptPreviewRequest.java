package com.itda.backend.node.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * 노드 프롬프트 미리보기 요청 DTO
 */
@Schema(description = "Node prompt preview request")
public record PromptPreviewRequest(

        @Schema(description = "Base English prompt", example = "A bright morning in a Mars base cafeteria.")
        String prompt,

        @Schema(description = "Node settings (style, ratio, etc.)")
        Map<String, Object> settings,

        @Schema(description = "Final English prompt override (optional). If provided, preview uses this as-is.")
        String promptEnFinalOverride
) {
}
