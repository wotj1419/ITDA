package com.itda.backend.timeline.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "Scene timeline reorder request")
public record ReorderSceneTimelineRequest(
        @Schema(description = "Ordered video node IDs", example = "[14, 15, 16]")
        @NotEmpty(message = "orderedVideoNodeIds is required")
        List<@NotNull Long> orderedVideoNodeIds
) {
}
