package com.itda.backend.project.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Member invite request")
public record MemberInviteRequest(

        @Schema(description = "Invitee email", example = "newbie@itda.com")
        @NotBlank(message = "email is required")
        @Email(message = "email must be a valid email address")
        String email,

        @Schema(description = "Role to assign", example = "EDITOR")
        @NotBlank(message = "role is required")
        String role
) {
}
