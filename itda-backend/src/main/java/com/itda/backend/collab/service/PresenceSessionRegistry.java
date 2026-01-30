package com.itda.backend.collab.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PresenceSessionRegistry {

    public record PresenceSession(
            Long projectId,
            Long userId,
            String name,
            String profileImageUrl
    ) {
    }

    private final Map<String, PresenceSession> sessions = new ConcurrentHashMap<>();

    public void upsert(String sessionId, Long projectId, Long userId, String name, String profileImageUrl) {
        if (sessionId == null || projectId == null || userId == null) {
            return;
        }
        sessions.put(sessionId, new PresenceSession(projectId, userId, name, profileImageUrl));
    }

    public PresenceSession remove(String sessionId) {
        if (sessionId == null) {
            return null;
        }
        return sessions.remove(sessionId);
    }
}
