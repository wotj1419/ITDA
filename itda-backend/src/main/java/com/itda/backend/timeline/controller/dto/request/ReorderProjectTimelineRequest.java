package com.itda.backend.timeline.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "Project timeline reorder request")
public record ReorderProjectTimelineRequest(
        @Schema(description = "Ordered scene video IDs", example = "[101, 102, 103]")
        @NotEmpty(message = "orderedSceneVideoIds is required")
        List<@NotNull Long> orderedSceneVideoIds
) {
}
