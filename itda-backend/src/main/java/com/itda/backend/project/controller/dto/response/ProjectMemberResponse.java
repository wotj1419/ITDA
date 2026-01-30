package com.itda.backend.project.controller.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Project member response")
public record ProjectMemberResponse(

        @Schema(description = "User ID", example = "1")
        Long userId,

        @Schema(description = "Email", example = "owner@itda.com")
        String email,

        @Schema(description = "Name", example = "Owner User")
        String name,

        @Schema(description = "Profile image URL", example = "https://example.com/profile.png")
        String profileImageUrl,

        @Schema(description = "Role", example = "OWNER")
        String role
) {
}
