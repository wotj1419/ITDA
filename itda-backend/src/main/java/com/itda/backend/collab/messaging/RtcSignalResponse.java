package com.itda.backend.collab.messaging;

import java.util.List;

public record RtcSignalResponse(
        String type,
        Long projectId,
        Long senderId,
        Long targetId,
        Object sdp,
        Object candidate,
        Boolean muted,
        List<RtcParticipant> participants
) {
    public static RtcSignalResponse joinAck(Long projectId, List<RtcParticipant> participants) {
        return new RtcSignalResponse("JOIN_ACK", projectId, null, null, null, null, null, participants);
    }

    public static RtcSignalResponse join(Long projectId, Long senderId, Boolean muted) {
        return new RtcSignalResponse("JOIN", projectId, senderId, null, null, null, muted, null);
    }

    public static RtcSignalResponse leave(Long projectId, Long senderId) {
        return new RtcSignalResponse("LEAVE", projectId, senderId, null, null, null, null, null);
    }

    public static RtcSignalResponse mute(Long projectId, Long senderId, boolean muted) {
        return new RtcSignalResponse("MUTE", projectId, senderId, null, null, null, muted, null);
    }

    public static RtcSignalResponse offer(Long projectId, Long senderId, Long targetId, Object sdp) {
        return new RtcSignalResponse("OFFER", projectId, senderId, targetId, sdp, null, null, null);
    }

    public static RtcSignalResponse answer(Long projectId, Long senderId, Long targetId, Object sdp) {
        return new RtcSignalResponse("ANSWER", projectId, senderId, targetId, sdp, null, null, null);
    }

    public static RtcSignalResponse candidate(Long projectId, Long senderId, Long targetId, Object candidate) {
        return new RtcSignalResponse("CANDIDATE", projectId, senderId, targetId, null, candidate, null, null);
    }
}
