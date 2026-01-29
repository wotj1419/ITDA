package com.itda.backend.collab.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

@Configuration
public class CollabStompErrorHandlerConfig {

    @Bean
    public StompSubProtocolErrorHandler stompSubProtocolErrorHandler() {
        return new CollabStompErrorHandler();
    }
}
