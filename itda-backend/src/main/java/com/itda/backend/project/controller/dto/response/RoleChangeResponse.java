package com.itda.backend.project.controller.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Member role change response")
public record RoleChangeResponse(

        @Schema(description = "User ID", example = "5")
        Long userId,

        @Schema(description = "Previous role", example = "EDITOR")
        String previousRole,

        @Schema(description = "New role", example = "ADMIN")
        String newRole
) {
}
