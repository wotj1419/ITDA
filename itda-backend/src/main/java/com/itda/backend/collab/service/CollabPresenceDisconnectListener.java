package com.itda.backend.collab.service;

import com.itda.backend.collab.messaging.PresenceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class CollabPresenceDisconnectListener {

    private final PresenceSessionRegistry presenceSessionRegistry;
    private final CollabRedisPublisher collabRedisPublisher;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        if (event == null) {
            return;
        }
        String sessionId = event.getSessionId();
        PresenceSessionRegistry.PresenceRemoval removal = presenceSessionRegistry.remove(sessionId);
        if (removal == null || !removal.removed()) {
            return;
        }
        PresenceSessionRegistry.PresenceSession session = removal.session();
        if (session == null) {
            return;
        }

        PresenceEvent leave = PresenceEvent.leave(
                session.userId(),
                session.name(),
                session.profileImageUrl()
        );

        String destination = "/topic/presence/" + session.projectId();
        try {
            collabRedisPublisher.publish(destination, leave);
        } catch (Exception e) {
            log.debug("Presence leave redis publish failed. Falling back to in-memory broker.", e);
            messagingTemplate.convertAndSend(destination, leave);
        }
    }
}
