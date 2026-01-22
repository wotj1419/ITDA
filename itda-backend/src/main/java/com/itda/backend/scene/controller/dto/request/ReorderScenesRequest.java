package com.itda.backend.scene.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "Scene reorder request")
public record ReorderScenesRequest(

        @Schema(description = "Ordered scene IDs", example = "[202, 201, 203]")
        @NotEmpty(message = "orderedSceneIds is required")
        List<@NotNull Long> orderedSceneIds
) {
}
