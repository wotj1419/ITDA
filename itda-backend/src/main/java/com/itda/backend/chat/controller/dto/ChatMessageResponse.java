package com.itda.backend.chat.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Chat message response")
public record ChatMessageResponse(

        @Schema(description = "Message ID", example = "12345")
        Long messageId,

        @Schema(description = "Project ID", example = "101")
        Long projectId,

        @Schema(description = "Sender info")
        ChatMessageSender sender,

        @Schema(description = "Message content", example = "Hello!")
        String content,

        @Schema(description = "Message type", example = "TEXT")
        String type,

        @Schema(description = "Created at", example = "2026-01-28T15:00:00")
        LocalDateTime createdAt
) {
}
