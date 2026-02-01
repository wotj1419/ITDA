package com.itda.backend.collab.controller;

import com.itda.backend.auth.domain.User;
import com.itda.backend.auth.repository.UserMapper;
import com.itda.backend.collab.messaging.PresenceEvent;
import com.itda.backend.collab.messaging.PresenceRequest;
import com.itda.backend.collab.messaging.PresenceSnapshotResponse;
import com.itda.backend.collab.service.CollabRedisPublisher;
import com.itda.backend.collab.service.PresenceSessionRegistry;
import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import com.itda.backend.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class CollabPresenceController {

    private final UserMapper userMapper;
    private final CollabRedisPublisher collabRedisPublisher;
    private final SimpMessagingTemplate messagingTemplate;
    private final PresenceSessionRegistry presenceSessionRegistry;

    @MessageMapping("/presence/{projectId}")
    public void handlePresence(
            @DestinationVariable Long projectId,
            PresenceRequest request,
            @Header("simpSessionId") String sessionId,
            Principal principal
    ) {
        CustomUserDetails userDetails = extractUser(principal);
        Long userId = userDetails.getUserId();

        User user = userMapper.findById(userId).orElse(null);
        String name = user != null && user.getName() != null ? user.getName() : userDetails.getUsername();
        String profileImageUrl = user != null ? user.getProfileImageUrl() : null;

        PresenceEvent event = PresenceEvent.location(userId, name, profileImageUrl, request);
        boolean isNewSession = presenceSessionRegistry.upsert(
                sessionId,
                projectId,
                userId,
                name,
                profileImageUrl,
                event
        );
        String destination = "/topic/presence/" + projectId;

        try {
            collabRedisPublisher.publish(destination, event);
        } catch (Exception e) {
            log.debug("Presence redis publish failed. Falling back to in-memory broker.", e);
            messagingTemplate.convertAndSend(destination, event);
        }

        if (isNewSession) {
            PresenceSnapshotResponse snapshot = PresenceSnapshotResponse.of(
                    projectId,
                    presenceSessionRegistry.snapshot(projectId)
            );
            String userDestination = "/queue/presence";
            String userKey = userDetails.getUsername();
            try {
                collabRedisPublisher.publishToUser(userKey, userDestination, snapshot);
            } catch (Exception e) {
                log.debug("Presence snapshot redis publish failed. Falling back to in-memory broker.", e);
                messagingTemplate.convertAndSendToUser(userKey, userDestination, snapshot);
            }
        }
    }

    private CustomUserDetails extractUser(Principal principal) {
        if (principal instanceof Authentication authentication &&
                authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails;
        }
        throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
}
