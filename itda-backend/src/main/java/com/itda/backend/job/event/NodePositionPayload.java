package com.itda.backend.job.event;

/**
 * Lightweight node position payload for WebSocket events.
 *
 * @param nodeId node id
 * @param x      x position
 * @param y      y position
 */
public record NodePositionPayload(
        Long nodeId,
        Float x,
        Float y
) {
}
