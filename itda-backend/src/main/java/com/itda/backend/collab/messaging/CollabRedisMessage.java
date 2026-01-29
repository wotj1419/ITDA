package com.itda.backend.collab.messaging;

public record CollabRedisMessage(
        String destination,
        String payload,
        String userId
) {
}
