package com.itda.backend.collab.service;

import com.itda.backend.collab.messaging.RtcParticipant;
import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RtcRoomManager {

    public static final int MAX_PARTICIPANTS = 6;

    public record RtcSession(Long projectId, Long userId) {
    }

    public record JoinResult(boolean newParticipant, List<RtcParticipant> participants) {
    }

    public record LeaveResult(Long projectId, RtcParticipant participant, boolean removed) {
    }

    public record RtcParticipantState(
            Long userId,
            String userKey,
            String name,
            String profileImageUrl,
            boolean muted
    ) {
        public RtcParticipant toParticipant() {
            return new RtcParticipant(userId, name, profileImageUrl, muted);
        }

        public RtcParticipantState withMuted(boolean nextMuted) {
            return new RtcParticipantState(userId, userKey, name, profileImageUrl, nextMuted);
        }
    }

    private final Map<String, RtcSession> sessions = new ConcurrentHashMap<>();
    private final Map<Long, Map<Long, RtcParticipantState>> projectParticipants = new ConcurrentHashMap<>();
    private final Map<Long, Map<Long, Integer>> projectSessionCounts = new ConcurrentHashMap<>();
    private final Map<Long, Object> projectLocks = new ConcurrentHashMap<>();

    public JoinResult join(String sessionId, Long projectId, RtcParticipantState participant) {
        if (sessionId == null || projectId == null || participant == null || participant.userId() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        Object lock = lockFor(projectId);
        synchronized (lock) {
            RtcSession previousSession = sessions.put(sessionId, new RtcSession(projectId, participant.userId()));
            boolean sameSession = previousSession != null
                    && previousSession.projectId().equals(projectId)
                    && previousSession.userId().equals(participant.userId());

            if (!sameSession && previousSession != null) {
                decrementSession(previousSession.projectId(), previousSession.userId());
            }

            Map<Long, Integer> counts = projectSessionCounts.computeIfAbsent(projectId, key -> new ConcurrentHashMap<>());
            Map<Long, RtcParticipantState> participants =
                    projectParticipants.computeIfAbsent(projectId, key -> new ConcurrentHashMap<>());

            boolean alreadyParticipant = participants.containsKey(participant.userId());
            if (!alreadyParticipant && counts.size() >= MAX_PARTICIPANTS) {
                if (!sameSession) {
                    sessions.remove(sessionId);
                }
                throw new BusinessException(ErrorCode.RTC_ROOM_FULL);
            }

            if (!sameSession) {
                counts.merge(participant.userId(), 1, Integer::sum);
            }

            participants.put(participant.userId(), participant);

            List<RtcParticipant> others = new ArrayList<>();
            for (RtcParticipantState state : participants.values()) {
                if (!state.userId().equals(participant.userId())) {
                    others.add(state.toParticipant());
                }
            }
            return new JoinResult(!alreadyParticipant, others);
        }
    }

    public LeaveResult leave(String sessionId) {
        if (sessionId == null) {
            return null;
        }
        RtcSession session = sessions.remove(sessionId);
        if (session == null) {
            return null;
        }

        Object lock = lockFor(session.projectId());
        synchronized (lock) {
            RtcParticipantState state = getParticipantState(session.projectId(), session.userId());
            boolean removed = decrementSession(session.projectId(), session.userId());
            RtcParticipant participant = state == null ? null : state.toParticipant();
            return new LeaveResult(session.projectId(), participant, removed);
        }
    }

    public RtcParticipantState getParticipantState(Long projectId, Long userId) {
        if (projectId == null || userId == null) {
            return null;
        }
        Map<Long, RtcParticipantState> participants = projectParticipants.get(projectId);
        if (participants == null) {
            return null;
        }
        return participants.get(userId);
    }

    public List<RtcParticipant> listParticipants(Long projectId, Long excludeUserId) {
        Map<Long, RtcParticipantState> participants = projectParticipants.get(projectId);
        if (participants == null || participants.isEmpty()) {
            return List.of();
        }
        List<RtcParticipant> result = new ArrayList<>();
        for (RtcParticipantState state : participants.values()) {
            if (excludeUserId != null && excludeUserId.equals(state.userId())) {
                continue;
            }
            result.add(state.toParticipant());
        }
        return result;
    }

    public List<String> listUserKeys(Long projectId, Long excludeUserId) {
        Map<Long, RtcParticipantState> participants = projectParticipants.get(projectId);
        if (participants == null || participants.isEmpty()) {
            return List.of();
        }
        List<String> keys = new ArrayList<>();
        for (RtcParticipantState state : participants.values()) {
            if (excludeUserId != null && excludeUserId.equals(state.userId())) {
                continue;
            }
            if (state.userKey() != null && !state.userKey().isBlank()) {
                keys.add(state.userKey());
            }
        }
        return keys;
    }

    public void updateMute(Long projectId, Long userId, boolean muted) {
        if (projectId == null || userId == null) {
            return;
        }
        Object lock = lockFor(projectId);
        synchronized (lock) {
            Map<Long, RtcParticipantState> participants = projectParticipants.get(projectId);
            if (participants == null) {
                return;
            }
            RtcParticipantState current = participants.get(userId);
            if (current == null) {
                return;
            }
            participants.put(userId, current.withMuted(muted));
        }
    }

    public String resolveUserKey(Long projectId, Long userId) {
        RtcParticipantState state = getParticipantState(projectId, userId);
        return state == null ? null : state.userKey();
    }

    private boolean decrementSession(Long projectId, Long userId) {
        Map<Long, Integer> counts = projectSessionCounts.get(projectId);
        if (counts == null) {
            return false;
        }
        Integer current = counts.get(userId);
        if (current == null) {
            return false;
        }
        if (current <= 1) {
            counts.remove(userId);
            Map<Long, RtcParticipantState> participants = projectParticipants.get(projectId);
            if (participants != null) {
                participants.remove(userId);
                if (participants.isEmpty()) {
                    projectParticipants.remove(projectId);
                }
            }
            if (counts.isEmpty()) {
                projectSessionCounts.remove(projectId);
            }
            return true;
        }
        counts.put(userId, current - 1);
        return false;
    }

    private Object lockFor(Long projectId) {
        return projectLocks.computeIfAbsent(projectId, key -> new Object());
    }
}
