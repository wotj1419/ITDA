package com.itda.backend.collab.messaging;

/**
 * Presence update payload received from clients.
 */
public record PresenceRequest(
        String type,
        String location,
        Long sceneId,
        Long nodeId
) {
}
