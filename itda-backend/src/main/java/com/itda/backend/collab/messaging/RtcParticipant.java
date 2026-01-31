package com.itda.backend.collab.messaging;

public record RtcParticipant(
        Long userId,
        String name,
        String profileImageUrl,
        Boolean muted
) {
}
