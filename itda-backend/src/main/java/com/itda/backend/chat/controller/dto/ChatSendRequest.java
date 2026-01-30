package com.itda.backend.chat.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Chat send request")
public record ChatSendRequest(

        @Schema(description = "Message content", example = "Hello!")
        @NotBlank
        @Size(max = 2000)
        String content,

        @Schema(description = "Message type", example = "TEXT")
        String type
) {
}
