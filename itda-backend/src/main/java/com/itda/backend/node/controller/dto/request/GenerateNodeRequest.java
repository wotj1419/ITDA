package com.itda.backend.node.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * 노드 생성 요청 (AI 생성)
 */
@Schema(description = "Node generate request")
public record GenerateNodeRequest(

        @Schema(description = "AI prompt", example = "화성 기지의 아침 식사 장면")
        String prompt,

        @Schema(description = "Node settings (style, ratio, etc.)")
        Map<String, Object> settings,

        @Schema(description = "Force regenerate even if same request", defaultValue = "false")
        boolean force
) {
}
