package com.itda.backend.chat.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Chat message sender")
public record ChatMessageSender(

        @Schema(description = "Sender user ID", example = "5")
        Long userId,

        @Schema(description = "Sender name", example = "Alex")
        String name,

        @Schema(description = "Sender profile image URL", example = "https://example.com/avatar.png")
        String profileImageUrl
) {
}
