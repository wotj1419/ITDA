package com.itda.backend.collab.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Optional;

/**
 * 협업(WebRTC 시그널링/채팅/presence/cursor)용 Raw WebSocket 핸들러 (v0)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CollabWebSocketHandler extends TextWebSocketHandler {

    private static final int PROTOCOL_VERSION = 0;
    private static final String ATTR_ROOM_ID = "collab.roomId";
    private static final String ATTR_CLIENT_ID = "collab.clientId";

    private final ObjectMapper objectMapper;
    private final CollabRoomManager roomManager;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String roomId = extractRoomId(session);
        session.getAttributes().put(ATTR_ROOM_ID, roomId);
        log.info("[collab] connected sessionId={}, roomId={}", session.getId(), roomId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode root = objectMapper.readTree(message.getPayload());
        String type = Optional.ofNullable(root.get("type")).map(JsonNode::asText).orElse(null);

        if (type == null || type.isBlank()) {
            sendError(session, "missing_type", "Message 'type' is required.");
            return;
        }

        String roomId = (String) session.getAttributes().get(ATTR_ROOM_ID);
        if (roomId == null || roomId.isBlank()) {
            sendError(session, "missing_room", "RoomId is missing from session.");
            return;
        }

        if ("hello".equals(type)) {
            handleHello(session, roomId, root);
            return;
        }

        String clientId = (String) session.getAttributes().get(ATTR_CLIENT_ID);
        if (clientId == null || clientId.isBlank()) {
            sendError(session, "hello_required", "Send 'hello' before other messages.");
            return;
        }

        switch (type) {
            case "webrtc.offer", "webrtc.answer", "webrtc.ice" -> handleRelay(session, roomId, clientId, root, type);
            case "chat.send" -> handleChat(session, roomId, clientId, root);
            case "cursor.move" -> handleCursor(session, roomId, clientId, root);
            case "presence.update" -> handlePresenceUpdate(session, roomId, clientId, root);
            case "ping" -> sendPong(session, clientId);
            default -> sendError(session, "unknown_type", "Unknown type: " + type);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        CollabRoomManager.LeaveResult leaveResult = roomManager.leave(session);
        if (leaveResult != null) {
            try {
                ObjectNode leaveData = leaveResult.leaver().toJson(objectMapper);
                roomManager.broadcast(
                        leaveResult.roomId(),
                        envelope("presence.leave", null, leaveResult.leaver(), leaveData),
                        null
                );
            } catch (Exception e) {
                log.warn("[collab] failed to broadcast leave", e);
            }
        }
        log.info("[collab] disconnected sessionId={}, status={}", session.getId(), status);
    }

    private void handleHello(WebSocketSession session, String roomId, JsonNode root) throws Exception {
        JsonNode data = root.get("data");
        String requestedClientId = data != null && data.hasNonNull("clientId") ? data.get("clientId").asText() : null;
        String name = data != null && data.hasNonNull("name") ? data.get("name").asText() : null;

        CollabRoomManager.JoinResult joinResult = roomManager.join(roomId, session, requestedClientId, name);
        session.getAttributes().put(ATTR_CLIENT_ID, joinResult.self().clientId());

        ObjectNode welcomeData = objectMapper.createObjectNode();
        welcomeData.put("roomId", roomId);
        welcomeData.set("self", joinResult.self().toJson(objectMapper));
        welcomeData.set("peers", joinResult.peersJson(objectMapper));

        session.sendMessage(envelope("welcome", null, joinResult.self(), welcomeData));

        // notify others
        ObjectNode joinData = joinResult.self().toJson(objectMapper);
        roomManager.broadcast(roomId, envelope("presence.join", null, joinResult.self(), joinData), joinResult.self().clientId());
    }

    private void handleRelay(WebSocketSession session, String roomId, String senderId, JsonNode root, String type) throws Exception {
        String targetId = Optional.ofNullable(root.get("targetId")).map(JsonNode::asText).orElse(null);
        if (targetId == null || targetId.isBlank()) {
            sendError(session, "missing_target", "targetId is required for " + type);
            return;
        }
        JsonNode data = root.get("data");
        CollabRoomManager.Participant sender = roomManager.getParticipant(roomId, senderId);
        if (sender == null) {
            sendError(session, "not_in_room", "Sender is not in room.");
            return;
        }
        boolean ok = roomManager.sendTo(roomId, targetId, envelope(type, null, sender, data));
        if (!ok) {
            sendError(session, "target_not_found", "targetId not found: " + targetId);
        }
    }

    private void handleChat(WebSocketSession session, String roomId, String senderId, JsonNode root) throws Exception {
        JsonNode data = root.get("data");
        if (data == null || !data.hasNonNull("content")) {
            sendError(session, "invalid_chat", "data.content is required.");
            return;
        }
        CollabRoomManager.Participant sender = roomManager.getParticipant(roomId, senderId);
        if (sender == null) {
            sendError(session, "not_in_room", "Sender is not in room.");
            return;
        }
        roomManager.broadcast(roomId, envelope("chat.message", null, sender, data), null);
    }

    private void handleCursor(WebSocketSession session, String roomId, String senderId, JsonNode root) throws Exception {
        JsonNode data = root.get("data");
        if (data == null || !data.hasNonNull("x") || !data.hasNonNull("y")) {
            sendError(session, "invalid_cursor", "data.x and data.y are required.");
            return;
        }
        CollabRoomManager.Participant sender = roomManager.getParticipant(roomId, senderId);
        if (sender == null) {
            sendError(session, "not_in_room", "Sender is not in room.");
            return;
        }
        roomManager.updateCursor(roomId, senderId, data);
        roomManager.broadcast(roomId, envelope("cursor.update", null, sender, data), senderId);
    }

    private void handlePresenceUpdate(WebSocketSession session, String roomId, String senderId, JsonNode root) throws Exception {
        JsonNode data = root.get("data");
        if (data == null || !data.isObject()) {
            sendError(session, "invalid_presence", "data(object) is required.");
            return;
        }
        CollabRoomManager.Participant sender = roomManager.getParticipant(roomId, senderId);
        if (sender == null) {
            sendError(session, "not_in_room", "Sender is not in room.");
            return;
        }
        roomManager.updatePresence(roomId, senderId, data);
        roomManager.broadcast(roomId, envelope("presence.update", null, sender, data), senderId);
    }

    private void sendPong(WebSocketSession session, String clientId) throws Exception {
        CollabRoomManager.Participant sender = new CollabRoomManager.Participant(clientId, "pong", null);
        session.sendMessage(envelope("pong", null, sender, objectMapper.createObjectNode()));
    }

    private void sendError(WebSocketSession session, String code, String message) throws Exception {
        ObjectNode data = objectMapper.createObjectNode();
        data.put("code", code);
        data.put("message", message);
        CollabRoomManager.Participant server = new CollabRoomManager.Participant("server", "server", null);
        session.sendMessage(envelope("error", null, server, data));
    }

    private TextMessage envelope(String type, String targetId, CollabRoomManager.Participant sender, JsonNode data) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("v", PROTOCOL_VERSION);
        root.put("type", type);
        if (targetId != null) {
            root.put("targetId", targetId);
        }
        root.set("sender", sender.toJson(objectMapper));
        root.set("data", data == null ? objectMapper.createObjectNode() : data);
        root.put("ts", System.currentTimeMillis());
        return new TextMessage(objectMapper.writeValueAsString(root));
    }

    private String extractRoomId(WebSocketSession session) {
        if (session.getUri() == null) {
            return "unknown";
        }
        String path = session.getUri().getPath();
        if (path == null) {
            return "unknown";
        }
        String[] parts = path.split("/");
        return parts.length == 0 ? "unknown" : parts[parts.length - 1];
    }
}
