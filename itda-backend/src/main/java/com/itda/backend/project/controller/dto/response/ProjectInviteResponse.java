package com.itda.backend.project.controller.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Project invite response")
public record ProjectInviteResponse(

        @Schema(description = "Invite ID", example = "10")
        Long inviteId,

        @Schema(description = "Project ID", example = "101")
        Long projectId,

        @Schema(description = "Project title", example = "Mars Vlog")
        String projectTitle,

        @Schema(description = "Project genre", example = "Sci-Fi")
        String projectGenre,

        @Schema(description = "Project thumbnail URL", example = "https://...")
        String projectThumbnailUrl,

        @Schema(description = "Sender name", example = "Owner User")
        String senderName,

        @Schema(description = "Sender email", example = "owner@itda.com")
        String senderEmail,

        @Schema(description = "Receiver email", example = "invitee@itda.com")
        String receiverEmail,

        @Schema(description = "Invite created at", example = "2026-01-15T12:00:00")
        LocalDateTime createdAt,

        @Schema(description = "Invite status", example = "PENDING")
        String status
) {
}
