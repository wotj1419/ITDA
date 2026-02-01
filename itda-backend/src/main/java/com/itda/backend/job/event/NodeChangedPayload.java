package com.itda.backend.job.event;

/**
 * Node change payload for project WebSocket events.
 *
 * @param sceneId scene id
 * @param nodeId  affected node id (nullable for scene-wide changes)
 * @param action  action hint (CREATED/UPDATED/DELETED/CONFIRMED/UNCONFIRMED/etc)
 * @param actorId user id who triggered the change
 * @param positions updated node positions (nullable)
 */
public record NodeChangedPayload(
        Long sceneId,
        Long nodeId,
        String action,
        Long actorId,
        java.util.List<NodePositionPayload> positions
) {
}
