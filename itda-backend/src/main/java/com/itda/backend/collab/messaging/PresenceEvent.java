package com.itda.backend.collab.messaging;

/**
 * Presence event broadcast to subscribers.
 */
public record PresenceEvent(
        String type,
        Long userId,
        String name,
        String profileImageUrl,
        String location,
        Long sceneId,
        Long nodeId
) {
    public static PresenceEvent location(Long userId, String name, String profileImageUrl, PresenceRequest request) {
        if (request == null) {
            return new PresenceEvent("LOCATION", userId, name, profileImageUrl, null, null, null);
        }
        return new PresenceEvent(
                "LOCATION",
                userId,
                name,
                profileImageUrl,
                request.location(),
                request.sceneId(),
                request.nodeId()
        );
    }

    public static PresenceEvent leave(Long userId, String name, String profileImageUrl) {
        return new PresenceEvent("LEAVE", userId, name, profileImageUrl, null, null, null);
    }
}
