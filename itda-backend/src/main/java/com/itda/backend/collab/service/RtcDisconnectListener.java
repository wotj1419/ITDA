package com.itda.backend.collab.service;

import com.itda.backend.collab.messaging.RtcSignalResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RtcDisconnectListener {

    private static final String RTC_USER_DESTINATION = "/queue/rtc";

    private final RtcRoomManager rtcRoomManager;
    private final CollabRedisPublisher collabRedisPublisher;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        if (event == null) {
            return;
        }
        String sessionId = event.getSessionId();
        RtcRoomManager.LeaveResult result = rtcRoomManager.leave(sessionId);
        if (result == null || !result.removed() || result.participant() == null) {
            return;
        }

        Long projectId = result.projectId();
        Long userId = result.participant().userId();
        RtcSignalResponse leave = RtcSignalResponse.leave(projectId, userId);

        List<String> userKeys = rtcRoomManager.listUserKeys(projectId, userId);
        if (userKeys.isEmpty()) {
            return;
        }
        for (String userKey : userKeys) {
            sendToUser(userKey, RTC_USER_DESTINATION, leave);
        }
    }

    private void sendToUser(String userKey, String destination, Object payload) {
        try {
            collabRedisPublisher.publishToUser(userKey, destination, payload);
        } catch (Exception e) {
            log.debug("RTC disconnect publish failed. Falling back to in-memory broker.", e);
            messagingTemplate.convertAndSendToUser(userKey, destination, payload);
        }
    }
}
