package com.itda.backend.chat.controller;

import com.itda.backend.chat.controller.dto.ChatMessagePage;
import com.itda.backend.chat.service.ChatService;
import com.itda.backend.global.response.ApiResponse;
import com.itda.backend.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Chat", description = "Chat history API")
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ChatHistoryController {

    private final ChatService chatService;

    @Operation(summary = "Get chat messages", description = "Fetch chat history for a project.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Fetch success",
                    content = @Content(schema = @Schema(implementation = ChatMessagePage.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Project not found")
    })
    @GetMapping("/{projectId}/chat/messages")
    public ResponseEntity<ApiResponse<ChatMessagePage>> getMessages(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "Project ID") @PathVariable Long projectId,
            @RequestParam(defaultValue = "50") Integer size,
            @RequestParam(required = false) Long before
    ) {
        ChatMessagePage response = chatService.getMessages(projectId, userDetails.getUserId(), size, before);
        return ApiResponse.success(response);
    }
}
