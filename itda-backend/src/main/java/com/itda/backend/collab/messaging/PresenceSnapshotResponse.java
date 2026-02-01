package com.itda.backend.collab.messaging;

import java.util.List;

public record PresenceSnapshotResponse(
        String type,
        Long projectId,
        List<PresenceEvent> participants
) {
    public static PresenceSnapshotResponse of(Long projectId, List<PresenceEvent> participants) {
        return new PresenceSnapshotResponse("SNAPSHOT", projectId, participants);
    }
}
