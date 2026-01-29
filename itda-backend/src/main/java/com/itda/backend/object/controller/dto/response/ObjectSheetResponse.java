package com.itda.backend.object.controller.dto.response;

import com.itda.backend.object.domain.ObjectSheet;
import com.itda.backend.object.domain.ObjectStatus;
import com.itda.backend.object.domain.ObjectType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Object sheet response")
public record ObjectSheetResponse(

        @Schema(description = "Object ID", example = "301")
        Long objectId,

        @Schema(description = "Project ID", example = "101")
        Long projectId,

        @Schema(description = "Object name", example = "우주인 민준")
        String name,

        @Schema(description = "Object type", example = "CHARACTER")
        ObjectType type,

        @Schema(description = "Object description")
        String description,

        @Schema(description = "Art style", example = "SF 실사")
        String style,

        @Schema(description = "Sheet image URL", example = "https://.../object-101.png")
        String sheetImageUrl,

        @Schema(description = "Generation status", example = "PENDING")
        ObjectStatus status
) {
    public static ObjectSheetResponse from(ObjectSheet objectSheet) {
        return new ObjectSheetResponse(
                objectSheet.getId(),
                objectSheet.getProjectId(),
                objectSheet.getName(),
                objectSheet.getType(),
                objectSheet.getDescription(),
                objectSheet.getStyle(),
                objectSheet.getSheetImageUrl(),
                objectSheet.getStatus()
        );
    }
}
