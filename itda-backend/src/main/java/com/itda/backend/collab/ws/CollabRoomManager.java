package com.itda.backend.collab.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class CollabRoomManager {

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, ClientRef> sessionIndex = new ConcurrentHashMap<>();

    public record Participant(String clientId, String name, PresenceState state) {
        ObjectNode toJson(ObjectMapper mapper) {
            ObjectNode node = mapper.createObjectNode();
            node.put("clientId", clientId);
            node.put("name", name);
            if (state != null) {
                node.set("state", state.toJson(mapper));
            }
            return node;
        }
    }

    public record JoinResult(Participant self, List<Participant> peers) {
        ArrayNode peersJson(ObjectMapper mapper) {
            ArrayNode array = mapper.createArrayNode();
            for (Participant p : peers) {
                array.add(p.toJson(mapper));
            }
            return array;
        }
    }

    public record LeaveResult(String roomId, Participant leaver) {
    }

    private static final class ClientRef {
        private final String roomId;
        private final String clientId;

        private ClientRef(String roomId, String clientId) {
            this.roomId = roomId;
            this.clientId = clientId;
        }
    }

    private static final class Room {
        private final Map<String, Client> clients = new ConcurrentHashMap<>();
    }

    private static final class Client {
        private final String clientId;
        private final WebSocketSession session;
        private volatile String name;
        private final PresenceState state = new PresenceState();

        private Client(String clientId, String name, WebSocketSession session) {
            this.clientId = clientId;
            this.name = name;
            this.session = session;
        }
    }

    public static final class PresenceState {
        private volatile boolean isMuted = true;
        private volatile boolean isVideoOff = true;
        private volatile boolean isScreenSharing = false;
        private volatile String currentLocation = "";
        private volatile Double cursorX = null;
        private volatile Double cursorY = null;

        ObjectNode toJson(ObjectMapper mapper) {
            ObjectNode node = mapper.createObjectNode();
            node.put("isMuted", isMuted);
            node.put("isVideoOff", isVideoOff);
            node.put("isScreenSharing", isScreenSharing);
            node.put("currentLocation", currentLocation);
            if (cursorX != null && cursorY != null) {
                ObjectNode cursor = mapper.createObjectNode();
                cursor.put("x", cursorX);
                cursor.put("y", cursorY);
                node.set("cursor", cursor);
            }
            return node;
        }
    }

    public JoinResult join(String roomId, WebSocketSession session, String requestedClientId, String requestedName) {
        Room room = rooms.computeIfAbsent(roomId, _ignored -> new Room());
        String clientId = allocateClientId(room, requestedClientId);
        String name = (requestedName == null || requestedName.isBlank()) ? "User-" + clientId.substring(0, 4) : requestedName;

        Client client = new Client(clientId, name, session);
        room.clients.put(clientId, client);
        sessionIndex.put(session.getId(), new ClientRef(roomId, clientId));

        List<Participant> peers = new ArrayList<>();
        for (Client c : room.clients.values()) {
            if (!c.clientId.equals(clientId)) {
                peers.add(new Participant(c.clientId, c.name, c.state));
            }
        }

        return new JoinResult(new Participant(clientId, name, client.state), peers);
    }

    public LeaveResult leave(WebSocketSession session) {
        ClientRef ref = sessionIndex.remove(session.getId());
        if (ref == null) {
            return null;
        }

        Room room = rooms.get(ref.roomId);
        if (room == null) {
            return null;
        }

        Client removed = room.clients.remove(ref.clientId);
        if (removed == null) {
            return null;
        }

        if (room.clients.isEmpty()) {
            rooms.remove(ref.roomId);
        }

        return new LeaveResult(ref.roomId, new Participant(removed.clientId, removed.name, removed.state));
    }

    public Participant getParticipant(String roomId, String clientId) {
        Room room = rooms.get(roomId);
        if (room == null) return null;
        Client client = room.clients.get(clientId);
        if (client == null) return null;
        return new Participant(client.clientId, client.name, client.state);
    }

    public boolean sendTo(String roomId, String targetClientId, TextMessage message) {
        Room room = rooms.get(roomId);
        if (room == null) return false;
        Client client = room.clients.get(targetClientId);
        if (client == null) return false;
        safeSend(client.session, message);
        return true;
    }

    public void broadcast(String roomId, TextMessage message, String excludeClientId) {
        Room room = rooms.get(roomId);
        if (room == null) return;
        for (Client c : room.clients.values()) {
            if (excludeClientId != null && excludeClientId.equals(c.clientId)) {
                continue;
            }
            safeSend(c.session, message);
        }
    }

    public void updatePresence(String roomId, String clientId, JsonNode data) {
        Room room = rooms.get(roomId);
        if (room == null) return;
        Client client = room.clients.get(clientId);
        if (client == null) return;

        if (data.has("isMuted")) client.state.isMuted = data.get("isMuted").asBoolean();
        if (data.has("isVideoOff")) client.state.isVideoOff = data.get("isVideoOff").asBoolean();
        if (data.has("isScreenSharing")) client.state.isScreenSharing = data.get("isScreenSharing").asBoolean();
        if (data.hasNonNull("currentLocation")) client.state.currentLocation = data.get("currentLocation").asText("");
    }

    public void updateCursor(String roomId, String clientId, JsonNode data) {
        Room room = rooms.get(roomId);
        if (room == null) return;
        Client client = room.clients.get(clientId);
        if (client == null) return;

        client.state.cursorX = data.get("x").asDouble();
        client.state.cursorY = data.get("y").asDouble();
    }

    private static String allocateClientId(Room room, String requestedClientId) {
        if (requestedClientId != null && !requestedClientId.isBlank() && !room.clients.containsKey(requestedClientId)) {
            return requestedClientId;
        }
        String id;
        do {
            id = UUID.randomUUID().toString();
        } while (room.clients.containsKey(id));
        return id;
    }

    private static void safeSend(WebSocketSession session, TextMessage message) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            session.sendMessage(message);
        } catch (IOException e) {
            log.warn("[collab] send failed sessionId={}", session.getId(), e);
        }
    }
}
