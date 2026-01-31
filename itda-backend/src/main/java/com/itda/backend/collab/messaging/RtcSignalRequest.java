package com.itda.backend.collab.messaging;

public record RtcSignalRequest(
        String type,
        Long projectId,
        Long targetId,
        Object sdp,
        Object candidate,
        Boolean muted
) {
}
