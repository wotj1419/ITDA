package com.itda.backend.object.controller.dto.request;

import com.itda.backend.object.domain.ObjectType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Object update request")
public record UpdateObjectRequest(

        @Schema(description = "Object name", example = "수정된 이름")
        @Size(max = 200, message = "name must be 200 characters or less")
        String name,

        @Schema(description = "Object type", example = "PROP", allowableValues = {"CHARACTER", "PROP", "ETC"})
        ObjectType type,

        @Schema(description = "Object description", example = "수정된 설명")
        @Size(max = 2000, message = "description must be 2000 characters or less")
        String description,

        @Schema(description = "Art style", example = "실사")
        @Size(max = 100, message = "style must be 100 characters or less")
        String style
) {
}
