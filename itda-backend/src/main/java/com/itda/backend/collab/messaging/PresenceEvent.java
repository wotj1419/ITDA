package com.itda.backend.collab.messaging;

import java.time.Instant;

/**
 * Presence event broadcast to subscribers.
 */
public record PresenceEvent(
        String type,
        Long userId,
        String name,
        String profileImageUrl,
        String location,
        String status,
        String summary,
        Long sceneId,
        Long nodeId,
        Double x,
        Double y,
        String action,
        Instant updatedAt
) {
    public static PresenceEvent location(Long userId, String name, String profileImageUrl, PresenceRequest request) {
        Instant now = Instant.now();
        if (request == null) {
            return new PresenceEvent("LOCATION", userId, name, profileImageUrl,
                    null, null, null, null, null, null, null, null, now);
        }
        return new PresenceEvent(
                "LOCATION",
                userId,
                name,
                profileImageUrl,
                request.location(),
                null,
                null,
                request.sceneId(),
                request.nodeId(),
                null,
                null,
                null,
                now
        );
    }

    public static PresenceEvent status(Long userId, String name, String profileImageUrl, PresenceRequest request) {
        Instant now = Instant.now();
        if (request == null) {
            return new PresenceEvent("STATUS", userId, name, profileImageUrl,
                    null, null, null, null, null, null, null, null, now);
        }
        return new PresenceEvent(
                "STATUS",
                userId,
                name,
                profileImageUrl,
                null,
                request.status(),
                request.summary(),
                request.sceneId(),
                request.nodeId(),
                null,
                null,
                null,
                now
        );
    }

    public static PresenceEvent cursor(Long userId, String name, String profileImageUrl, PresenceRequest request) {
        Instant now = Instant.now();
        if (request == null) {
            return new PresenceEvent("CURSOR", userId, name, profileImageUrl,
                    null, null, null, null, null, null, null, null, now);
        }
        return new PresenceEvent(
                "CURSOR",
                userId,
                name,
                profileImageUrl,
                null,
                null,
                null,
                request.sceneId(),
                null,
                request.x(),
                request.y(),
                null,
                now
        );
    }

    public static PresenceEvent nodeSelect(Long userId, String name, String profileImageUrl, PresenceRequest request) {
        Instant now = Instant.now();
        if (request == null) {
            return new PresenceEvent("NODE_SELECT", userId, name, profileImageUrl,
                    null, null, null, null, null, null, null, null, now);
        }
        return new PresenceEvent(
                "NODE_SELECT",
                userId,
                name,
                profileImageUrl,
                null,
                null,
                null,
                request.sceneId(),
                request.nodeId(),
                null,
                null,
                request.action(),
                now
        );
    }

    public static PresenceEvent nodeMove(Long userId, String name, String profileImageUrl, PresenceRequest request) {
        Instant now = Instant.now();
        if (request == null) {
            return new PresenceEvent("NODE_MOVE", userId, name, profileImageUrl,
                    null, null, null, null, null, null, null, null, now);
        }
        return new PresenceEvent(
                "NODE_MOVE",
                userId,
                name,
                profileImageUrl,
                null,
                null,
                null,
                request.sceneId(),
                request.nodeId(),
                request.x(),
                request.y(),
                null,
                now
        );
    }

    public static PresenceEvent leave(Long userId, String name, String profileImageUrl) {
        return new PresenceEvent("LEAVE", userId, name, profileImageUrl,
                null, null, null, null, null, null, null, null, Instant.now());
    }
}
