package com.itda.backend.node.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * 노드 수정 요청 DTO
 */
@Schema(description = "Node update request")
public record UpdateNodeRequest(

        @Schema(description = "AI prompt", example = "수정된 프롬프트")
        String prompt,

        @Schema(description = "Node settings (style, ratio, etc.)")
        Map<String, Object> settings
) {
}
