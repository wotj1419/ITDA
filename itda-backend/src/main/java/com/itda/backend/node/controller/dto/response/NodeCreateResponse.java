package com.itda.backend.node.controller.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 노드 생성 응답 DTO
 */
@Schema(description = "Node create response")
public record NodeCreateResponse(

        @Schema(description = "Created node ID", example = "301")
        Long nodeId
) {
}
