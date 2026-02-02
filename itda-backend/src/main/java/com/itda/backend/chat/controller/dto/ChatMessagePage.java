package com.itda.backend.chat.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Chat message page")
public record ChatMessagePage(

        @Schema(description = "Chat messages")
        List<ChatMessageResponse> items,

        @Schema(description = "Has more messages")
        boolean hasMore
) {
}
