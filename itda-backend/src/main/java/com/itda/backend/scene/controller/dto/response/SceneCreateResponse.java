package com.itda.backend.scene.controller.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Scene create response")
public record SceneCreateResponse(

        @Schema(description = "Scene ID", example = "201")
        Long sceneId,

        @Schema(description = "Scene title", example = "Scene 1: Mars Base")
        String title,

        @Schema(description = "Order index", example = "1")
        Integer order
) {
}
