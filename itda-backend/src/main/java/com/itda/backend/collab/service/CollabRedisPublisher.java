package com.itda.backend.collab.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itda.backend.collab.messaging.CollabRedisMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CollabRedisPublisher {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${collab.redis.channel:collab:ws}")
    private String channel;

    public void publish(String destination, Object payload) {
        publishInternal(destination, payload, null);
    }

    public void publishToUser(String userId, String destination, Object payload) {
        publishInternal(destination, payload, userId);
    }

    private void publishInternal(String destination, Object payload, String userId) {
        try {
            String payloadJson = payload instanceof String
                    ? (String) payload
                    : objectMapper.writeValueAsString(payload);
            CollabRedisMessage message = new CollabRedisMessage(destination, payloadJson, userId);
            String messageJson = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(channel, messageJson);
        } catch (JsonProcessingException e) {
            log.warn("Failed to publish collab message: destination={}, userId={}", destination, userId, e);
        }
    }
}
