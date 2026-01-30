package com.itda.backend.node.controller.dto.request;

import com.itda.backend.node.domain.NodeType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * 노드 생성 요청 (AI 생성)
 */
@Schema(description = "Node generate request")
public record GenerateNodeRequest(

        @Schema(description = "AI prompt", example = "화성 기지의 아침 식사 장면")
        String prompt,

        @Schema(description = "Node type (optional; server resolves from nodeId if omitted)", example = "MASTER")
        NodeType nodeType,

        @Schema(description = "Node settings (style, ratio, etc.)")
        Map<String, Object> settings,

        @Schema(description = "Final English prompt override (optional). If provided, generation uses this as-is.")
        String promptEnFinalOverride,

        @Schema(description = "Reference object IDs (optional)", example = "[1, 2, 3]")
        List<Long> referenceObjectIds,

        @Schema(description = "Idempotency key (optional). If provided, same key will return existing job.", example = "node-generate-001")
        String idempotencyKey,

        @Schema(description = "Requeue if existing job is pending/failed (optional).", example = "false")
        Boolean requeueIfExisting,

        @Schema(description = "Force regenerate even if same request", defaultValue = "false")
        boolean force
) {
}
