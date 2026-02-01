package com.itda.backend.collab.controller;

import com.itda.backend.auth.domain.User;
import com.itda.backend.auth.repository.UserMapper;
import com.itda.backend.collab.messaging.RtcErrorMessage;
import com.itda.backend.collab.messaging.RtcSignalRequest;
import com.itda.backend.collab.messaging.RtcSignalResponse;
import com.itda.backend.collab.service.CollabRedisPublisher;
import com.itda.backend.collab.service.RtcRoomManager;
import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import com.itda.backend.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;
import java.util.Locale;

@Slf4j
@Controller
@RequiredArgsConstructor
public class RtcSignalingController {

    private static final String RTC_USER_DESTINATION = "/queue/rtc";
    private static final String ERROR_USER_DESTINATION = "/queue/errors";

    private final UserMapper userMapper;
    private final CollabRedisPublisher collabRedisPublisher;
    private final SimpMessagingTemplate messagingTemplate;
    private final RtcRoomManager rtcRoomManager;

    @MessageMapping("/rtc/{projectId}")
    public void handleRtc(
            @DestinationVariable Long projectId,
            @Payload RtcSignalRequest request,
            @Header("simpSessionId") String sessionId,
            Principal principal
    ) {
        CustomUserDetails userDetails = extractUser(principal);
        Long userId = userDetails.getUserId();

        if (request == null || request.type() == null || request.type().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        String type = request.type().trim().toUpperCase(Locale.ROOT);
        if (request.projectId() != null && !request.projectId().equals(projectId)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        switch (type) {
            case "JOIN" -> handleJoin(projectId, sessionId, userDetails, userId);
            case "LEAVE" -> handleLeave(projectId, sessionId, userId);
            case "MUTE" -> handleMute(projectId, userId, request);
            case "OFFER" -> handleOffer(projectId, userId, request);
            case "ANSWER" -> handleAnswer(projectId, userId, request);
            case "CANDIDATE" -> handleCandidate(projectId, userId, request);
            default -> throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void handleJoin(Long projectId,
                            String sessionId,
                            CustomUserDetails userDetails,
                            Long userId) {
        User user = userMapper.findById(userId).orElse(null);
        String name = user != null && user.getName() != null ? user.getName() : userDetails.getUsername();
        String profileImageUrl = user != null ? user.getProfileImageUrl() : null;
        String userKey = userDetails.getUsername();

        try {
            RtcRoomManager.JoinResult joinResult = rtcRoomManager.join(
                    sessionId,
                    projectId,
                    new RtcRoomManager.RtcParticipantState(userId, userKey, name, profileImageUrl, false)
            );
            sendToUser(
                    userKey,
                    RTC_USER_DESTINATION,
                    RtcSignalResponse.joinAck(projectId, joinResult.participants())
            );

            if (joinResult.newParticipant()) {
                RtcSignalResponse join = RtcSignalResponse.join(projectId, userId, false);
                broadcastToOthers(projectId, userId, join);
            }
        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.RTC_ROOM_FULL) {
                sendToUser(
                        userKey,
                        ERROR_USER_DESTINATION,
                        RtcErrorMessage.from(e.getErrorCode())
                );
                return;
            }
            throw e;
        }
    }

    private void handleLeave(Long projectId, String sessionId, Long userId) {
        RtcRoomManager.LeaveResult result = rtcRoomManager.leave(sessionId);
        if (result == null || !result.removed()) {
            return;
        }
        Long actualProjectId = result.projectId() != null ? result.projectId() : projectId;
        RtcSignalResponse leave = RtcSignalResponse.leave(actualProjectId, userId);
        broadcastToOthers(actualProjectId, userId, leave);
    }

    private void handleMute(Long projectId, Long userId, RtcSignalRequest request) {
        Boolean muted = request.muted();
        if (muted == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        rtcRoomManager.updateMute(projectId, userId, muted);
        RtcSignalResponse message = RtcSignalResponse.mute(projectId, userId, muted);
        broadcastToOthers(projectId, userId, message);
    }

    private void handleOffer(Long projectId, Long userId, RtcSignalRequest request) {
        if (request.targetId() == null || request.sdp() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        sendToTarget(projectId, request.targetId(), RtcSignalResponse.offer(projectId, userId, request.targetId(), request.sdp()));
    }

    private void handleAnswer(Long projectId, Long userId, RtcSignalRequest request) {
        if (request.targetId() == null || request.sdp() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        sendToTarget(projectId, request.targetId(), RtcSignalResponse.answer(projectId, userId, request.targetId(), request.sdp()));
    }

    private void handleCandidate(Long projectId, Long userId, RtcSignalRequest request) {
        if (request.targetId() == null || request.candidate() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        sendToTarget(projectId, request.targetId(), RtcSignalResponse.candidate(projectId, userId, request.targetId(), request.candidate()));
    }

    private void broadcastToOthers(Long projectId, Long excludeUserId, RtcSignalResponse message) {
        List<String> userKeys = rtcRoomManager.listUserKeys(projectId, excludeUserId);
        if (userKeys.isEmpty()) {
            return;
        }
        for (String userKey : userKeys) {
            sendToUser(userKey, RTC_USER_DESTINATION, message);
        }
    }

    private void sendToTarget(Long projectId, Long targetId, RtcSignalResponse message) {
        String userKey = rtcRoomManager.resolveUserKey(projectId, targetId);
        if (userKey == null || userKey.isBlank()) {
            log.debug("[RTC] Target not found: projectId={}, targetId={}", projectId, targetId);
            return;
        }
        sendToUser(userKey, RTC_USER_DESTINATION, message);
    }

    private void sendToUser(String userKey, String destination, Object payload) {
        try {
            collabRedisPublisher.publishToUser(userKey, destination, payload);
        } catch (Exception e) {
            log.debug("RTC redis publish failed. Falling back to in-memory broker.", e);
            messagingTemplate.convertAndSendToUser(userKey, destination, payload);
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
