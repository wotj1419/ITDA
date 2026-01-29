package com.itda.backend.collab.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itda.backend.collab.messaging.CollabRedisMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class CollabRedisSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String raw = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            CollabRedisMessage collabMessage = objectMapper.readValue(raw, CollabRedisMessage.class);
            if (collabMessage.userId() != null && !collabMessage.userId().isBlank()) {
                messagingTemplate.convertAndSendToUser(
                        collabMessage.userId(),
                        collabMessage.destination(),
                        collabMessage.payload()
                );
            } else {
                messagingTemplate.convertAndSend(collabMessage.destination(), collabMessage.payload());
            }
        } catch (Exception e) {
            log.warn("Failed to handle collab redis message: raw={}", raw, e);
        }
    }
}
