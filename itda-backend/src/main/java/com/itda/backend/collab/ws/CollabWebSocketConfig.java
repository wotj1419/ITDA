package com.itda.backend.collab.ws;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 협업(WebRTC 시그널링/채팅/presence/cursor)용 Raw WebSocket 설정
 *
 * Endpoint: /ws/room/{roomId}
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class CollabWebSocketConfig implements WebSocketConfigurer {

    private final CollabWebSocketHandler collabWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(collabWebSocketHandler, "/ws/room/*")
                .setAllowedOriginPatterns("*");
    }
}

