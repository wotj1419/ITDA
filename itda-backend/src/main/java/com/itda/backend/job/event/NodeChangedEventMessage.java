package com.itda.backend.job.event;

/**
 * Node change event message for project WebSocket channel.
 *
 * @param event event name (e.g. node.changed)
 * @param data  payload data
 */
public record NodeChangedEventMessage(
        String event,
        NodeChangedPayload data
) {
}
