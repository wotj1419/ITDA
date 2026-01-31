package com.itda.backend.collab.service;

import com.itda.backend.collab.messaging.PresenceEvent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
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

    public record PresenceRemoval(PresenceSession session, boolean removed) {
    }

    private final Map<String, PresenceSession> sessions = new ConcurrentHashMap<>();
    private final Map<Long, Map<Long, PresenceEvent>> projectPresence = new ConcurrentHashMap<>();
    private final Map<Long, Map<Long, Integer>> projectSessionCounts = new ConcurrentHashMap<>();

    public boolean upsert(
            String sessionId,
            Long projectId,
            Long userId,
            String name,
            String profileImageUrl,
            PresenceEvent event
    ) {
        if (sessionId == null || projectId == null || userId == null) {
            return false;
        }

        PresenceSession next = new PresenceSession(projectId, userId, name, profileImageUrl);
        PresenceSession previous = sessions.put(sessionId, next);
        boolean isNewSession = previous == null;

        if (previous != null && (!previous.projectId().equals(projectId) || !previous.userId().equals(userId))) {
            decrementSession(previous.projectId(), previous.userId());
        }
        if (previous == null || !previous.projectId().equals(projectId) || !previous.userId().equals(userId)) {
            incrementSession(projectId, userId);
        }

        if (event != null) {
            projectPresence
                    .computeIfAbsent(projectId, key -> new ConcurrentHashMap<>())
                    .put(userId, event);
        }

        return isNewSession;
    }

    public PresenceRemoval remove(String sessionId) {
        if (sessionId == null) {
            return null;
        }
        PresenceSession session = sessions.remove(sessionId);
        if (session == null) {
            return null;
        }
        boolean removed = decrementSession(session.projectId(), session.userId());
        return new PresenceRemoval(session, removed);
    }

    public List<PresenceEvent> snapshot(Long projectId) {
        if (projectId == null) {
            return List.of();
        }
        Map<Long, PresenceEvent> presenceMap = projectPresence.get(projectId);
        if (presenceMap == null || presenceMap.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(presenceMap.values());
    }

    private void incrementSession(Long projectId, Long userId) {
        Map<Long, Integer> counts = projectSessionCounts.computeIfAbsent(projectId, key -> new ConcurrentHashMap<>());
        counts.merge(userId, 1, Integer::sum);
    }

    private boolean decrementSession(Long projectId, Long userId) {
        Map<Long, Integer> counts = projectSessionCounts.get(projectId);
        if (counts == null) {
            return false;
        }
        Integer current = counts.get(userId);
        if (current == null) {
            return false;
        }
        if (current <= 1) {
            counts.remove(userId);
            Map<Long, PresenceEvent> presenceMap = projectPresence.get(projectId);
            if (presenceMap != null) {
                presenceMap.remove(userId);
                if (presenceMap.isEmpty()) {
                    projectPresence.remove(projectId);
                }
            }
            if (counts.isEmpty()) {
                projectSessionCounts.remove(projectId);
            }
            return true;
        }
        counts.put(userId, current - 1);
        return false;
    }
}
