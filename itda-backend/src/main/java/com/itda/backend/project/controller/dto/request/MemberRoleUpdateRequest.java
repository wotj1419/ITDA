package com.itda.backend.project.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Member role update request")
public record MemberRoleUpdateRequest(

        @Schema(description = "Role to assign", example = "ADMIN")
        @NotBlank(message = "role is required")
        String role
) {
}
