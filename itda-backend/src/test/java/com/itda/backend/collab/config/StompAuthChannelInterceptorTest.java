package com.itda.backend.collab.config;

import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import com.itda.backend.global.security.CustomUserDetails;
import com.itda.backend.global.security.JwtTokenProvider;
import com.itda.backend.project.service.ProjectAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StompAuthChannelInterceptorTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private ProjectAccessService projectAccessService;

    @Mock
    private MessageChannel messageChannel;

    private StompAuthChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new StompAuthChannelInterceptor(jwtTokenProvider, projectAccessService);
    }

    @Test
    @DisplayName("CONNECT에 토큰이 없으면 UNAUTHORIZED 예외를 던진다")
    void connectWithoutTokenThrowsUnauthorized() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        Message<byte[]> message = buildMessage(accessor);

        when(jwtTokenProvider.extractToken(null)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> interceptor.preSend(message, messageChannel));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("CONNECT에 유효 토큰이 있으면 Principal이 세팅된다")
    void connectWithValidTokenSetsPrincipal() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer access-token");
        Message<byte[]> message = buildMessage(accessor);

        when(jwtTokenProvider.extractToken("Bearer access-token")).thenReturn("access-token");
        when(jwtTokenProvider.parseAccessToken("access-token"))
                .thenReturn(new JwtTokenProvider.AccessTokenPayload(1L, "test@itda.com", "USER"));

        Message<?> result = interceptor.preSend(message, messageChannel);
        assertThat(result).isNotNull();
        Message<?> nonNullResult = Objects.requireNonNull(result);
        StompHeaderAccessor resultAccessor = MessageHeaderAccessor.getAccessor(nonNullResult, StompHeaderAccessor.class);

        assertThat(resultAccessor).isNotNull();
        StompHeaderAccessor nonNullAccessor = Objects.requireNonNull(resultAccessor);
        Object user = Objects.requireNonNull(nonNullAccessor.getUser());
        assertThat(user).isInstanceOf(Authentication.class);

        Authentication authentication = (Authentication) user;
        assertThat(authentication.getPrincipal()).isInstanceOf(CustomUserDetails.class);

        CustomUserDetails details = (CustomUserDetails) authentication.getPrincipal();
        assertThat(details.getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("SUBSCRIBE가 프로젝트 destination이면 멤버 체크를 수행한다")
    void subscribeWithProjectDestinationChecksMembership() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                new CustomUserDetails(1L, "test@itda.com", "USER"),
                null,
                List.of()
        );
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/chat/12");
        accessor.setUser(authentication);
        Message<byte[]> message = buildMessage(accessor);

        interceptor.preSend(message, messageChannel);

        verify(projectAccessService).ensureProjectAccessible(12L, 1L);
    }

    @Test
    @DisplayName("SUBSCRIBE가 프로젝트 destination이 아니면 멤버 체크를 하지 않는다")
    void subscribeWithoutProjectDestinationSkipsMembershipCheck() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                new CustomUserDetails(1L, "test@itda.com", "USER"),
                null,
                List.of()
        );
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/unknown");
        accessor.setUser(authentication);
        Message<byte[]> message = buildMessage(accessor);

        interceptor.preSend(message, messageChannel);

        verifyNoInteractions(projectAccessService);
    }

    @Test
    @DisplayName("SUBSCRIBE에 Principal이 없으면 UNAUTHORIZED 예외를 던진다")
    void subscribeWithoutPrincipalThrowsUnauthorized() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/chat/12");
        Message<byte[]> message = buildMessage(accessor);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> interceptor.preSend(message, messageChannel));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    private Message<byte[]> buildMessage(@NonNull StompHeaderAccessor accessor) {
        // preSend에서 user를 세팅할 수 있도록 mutable 헤더 유지
        accessor.setLeaveMutable(true);
        MessageHeaders headers = accessor.getMessageHeaders();
        return MessageBuilder.createMessage(new byte[0], headers);
    }
}
