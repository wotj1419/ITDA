package com.itda.backend.node.controller.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 노드 프롬프트 미리보기 응답 DTO
 */
@Schema(description = "Node prompt preview response")
public record PromptPreviewResponse(
        @Schema(description = "Final English prompt", example = "Cinematic modern film look...") 
        String promptEnFinal,

        @Schema(description = "Source of the final prompt (RENDERED or OVERRIDE)", example = "RENDERED")
        String source
) {
}
