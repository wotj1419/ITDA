package com.itda.backend.collab.config;

import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.lang.Nullable;

@Slf4j
public class CollabStompErrorHandler extends StompSubProtocolErrorHandler {

    @Override
    @Nullable
    @SuppressWarnings("NullableProblems")
    public Message<byte[]> handleClientMessageProcessingError(@Nullable Message<byte[]> clientMessage,
                                                              Throwable ex) {
        Throwable cause = unwrapCause(ex);
        ErrorCode errorCode = resolveErrorCode(cause);
        String message = errorCode.getCode() + ": " + errorCode.getMessage();

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
        accessor.setMessage(message);
        accessor.setLeaveMutable(true);

        log.debug("STOMP error frame sent: {}", message, cause);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Throwable unwrapCause(Throwable ex) {
        if (ex == null) {
            return null;
        }
        Throwable current = ex;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private ErrorCode resolveErrorCode(Throwable ex) {
        if (ex instanceof BusinessException businessException) {
            return businessException.getErrorCode();
        }
        return ErrorCode.INTERNAL_ERROR;
    }
}
