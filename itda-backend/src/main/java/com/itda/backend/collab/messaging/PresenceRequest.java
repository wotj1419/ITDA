package com.itda.backend.collab.messaging;

/**
 * Presence update payload received from clients.
 */
public record PresenceRequest(
        String type,
        String location,
        String status,
        String summary,
        Long sceneId,
        Long nodeId,
        Double x,
        Double y,
        String action
) {
}
