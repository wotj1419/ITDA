package com.itda.backend.scene.controller.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Scene export response")
public record SceneExportResponse(
        @Schema(description = "Asset ID", example = "123")
        Long assetId,
        @Schema(description = "Download URL", example = "/api/scenes/101/export/file")
        String downloadUrl,
        @Schema(description = "Merge signature", example = "f2c6b5...")
        String mergeSignature,
        @Schema(description = "Merge status", example = "COMPLETED")
        String status
) {
}
