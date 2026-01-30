package com.itda.backend.chat.controller;

import com.itda.backend.chat.controller.dto.ChatMessageResponse;
import com.itda.backend.chat.controller.dto.ChatSendRequest;
import com.itda.backend.chat.service.ChatService;
import com.itda.backend.collab.service.CollabRedisPublisher;
import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import com.itda.backend.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private static final String CHAT_TOPIC_PREFIX = "/topic/chat/";

    private final ChatService chatService;
    private final CollabRedisPublisher collabRedisPublisher;

    @MessageMapping("/chat/{projectId}")
    public void sendChat(@DestinationVariable Long projectId,
                         @Payload ChatSendRequest request,
                         Principal principal) {
        Long userId = extractUserId(principal);
        ChatMessageResponse response = chatService.createMessage(projectId, userId, request);
        collabRedisPublisher.publish(CHAT_TOPIC_PREFIX + projectId, response);
    }

    private Long extractUserId(Principal principal) {
        if (principal instanceof org.springframework.security.core.Authentication authentication &&
                authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getUserId();
        }
        throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
}
