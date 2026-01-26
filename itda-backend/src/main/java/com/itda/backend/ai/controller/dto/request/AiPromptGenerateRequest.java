package com.itda.backend.ai.controller.dto.request;

import com.itda.backend.node.domain.NodeType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * AI 프롬프트 생성 요청 DTO
 */
@Schema(description = "AI prompt generate request")
public record AiPromptGenerateRequest(
        @Schema(description = "Node type", example = "MASTER", allowableValues = {"MASTER", "GRID", "SHOT", "VIDEO"})
        NodeType nodeType,

        @Schema(description = "Scene one line", example = "사막 기지에서의 아침 식사")
        String sceneOneLine,

        @Schema(description = "Style", example = "cinematic, photorealistic")
        String style,

        @Schema(description = "Time of day", example = "sunrise")
        String timeOfDay,

        @Schema(description = "Mood", example = "hopeful")
        String mood,

        @Schema(description = "Objects", example = "[\"robot\", \"coffee\"]")
        List<String> objects
) {
}
