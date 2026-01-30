package com.itda.backend.object.controller.dto.request;

import com.itda.backend.object.domain.ObjectType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Object create request")
public record CreateObjectRequest(

        @Schema(description = "Object name", example = "우주인 민준")
        @NotBlank(message = "name must not be blank")
        @Size(max = 200, message = "name must be 200 characters or less")
        String name,

        @Schema(description = "Object type", example = "CHARACTER", allowableValues = {"CHARACTER", "PROP", "ETC"})
        @NotNull(message = "type must not be null")
        ObjectType type,

        @Schema(description = "Object description", example = "20대 후반 남성, 우주복 착용, 헬멧 벗음")
        @NotBlank(message = "description must not be blank")
        @Size(max = 2000, message = "description must be 2000 characters or less")
        String description,

        @Schema(description = "Art style", example = "SF 실사")
        @Size(max = 100, message = "style must be 100 characters or less")
        String style
) {
}
