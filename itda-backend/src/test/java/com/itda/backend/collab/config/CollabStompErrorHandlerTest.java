package com.itda.backend.collab.config;

import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class CollabStompErrorHandlerTest {

    @Test
    @DisplayName("BusinessException을 STOMP ERROR 프레임으로 변환한다")
    void businessExceptionIsMappedToStompErrorFrame() {
        CollabStompErrorHandler handler = new CollabStompErrorHandler();
        Message<byte[]> clientMessage = MessageBuilder.withPayload(new byte[0]).build();

        Message<byte[]> errorMessage = handler.handleClientMessageProcessingError(
                clientMessage,
                new BusinessException(ErrorCode.FORBIDDEN)
        );

        assertThat(errorMessage).isNotNull();
        Message<byte[]> nonNullErrorMessage = Objects.requireNonNull(errorMessage);
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(nonNullErrorMessage, StompHeaderAccessor.class);
        // BusinessException이 STOMP ERROR 프레임으로 매핑되는지 확인
        assertThat(accessor).isNotNull();
        StompHeaderAccessor nonNullAccessor = Objects.requireNonNull(accessor);
        assertThat(nonNullAccessor.getCommand()).isEqualTo(StompCommand.ERROR);
        assertThat(nonNullAccessor.getMessage()).isEqualTo("FORBIDDEN: 권한이 없습니다");
    }
}
