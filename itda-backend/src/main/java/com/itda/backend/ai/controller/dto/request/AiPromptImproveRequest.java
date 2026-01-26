package com.itda.backend.ai.controller.dto.request;

import com.itda.backend.node.domain.NodeType;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 프롬프트 개선 요청 DTO
 */
@Schema(description = "AI prompt improve request")
public record AiPromptImproveRequest(
        @Schema(description = "Node type", example = "VIDEO", allowableValues = {"MASTER", "GRID", "SHOT", "VIDEO"})
        NodeType nodeType,

        @Schema(description = "Current prompt", example = "화성 기지의 아침 식사 장면")
        String prompt,

        @Schema(description = "Improvement instruction", example = "조명과 카메라 구도를 더 구체적으로")
        String instruction
) {
}
