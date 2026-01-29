package com.itda.backend.collab.config;

import com.itda.backend.global.config.WebSocketConfig;
import com.itda.backend.global.security.JwtTokenProvider;
import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import com.itda.backend.project.service.ProjectAccessService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.lang.NonNull;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = StompAuthIntegrationTest.TestApp.class
)
@ActiveProfiles("test")
class StompAuthIntegrationTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration.class,
            org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
            org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
    })
    @Import({
            WebSocketConfig.class,
            CollabStompErrorHandlerConfig.class,
            StompAuthChannelInterceptor.class,
            JwtTokenProvider.class
    })
    static class TestApp {
        @Bean
        ProjectAccessService projectAccessService() {
            return Mockito.mock(ProjectAccessService.class);
        }
    }

    @LocalServerPort
    private int port;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ProjectAccessService projectAccessService;

    private WebSocketStompClient stompClient;

    @AfterEach
    void tearDown() {
        if (stompClient != null) {
            stompClient.stop();
        }
    }

    @Test
    @DisplayName("CONNECT에 Authorization이 없으면 세션 생성이 실패한다")
    void connectWithoutAuthorizationFailsSessionCreation() {
        stompClient = createStompClient();
        CompletableFuture<StompSession> connectFuture = stompClient.connectAsync(
                wsUrl(),
                new StompSessionHandlerAdapter() {
                }
        );

        Throwable thrown = null;
        try {
            connectFuture.get(3, TimeUnit.SECONDS);
        } catch (Exception ex) {
            thrown = ex;
        }

        assertThat(thrown)
                .as("CONNECT without Authorization should fail")
                .isNotNull();
    }

    @Test
    @DisplayName("SUBSCRIBE 시 프로젝트 멤버 체크가 호출된다")
    void subscribeTriggersProjectMembershipCheck() throws Exception {
        stompClient = createStompClient();
        String token = jwtTokenProvider.createAccessToken(1L, "test@itda.com", "USER");

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + token);

        StompSession session = stompClient.connectAsync(
                wsUrl(),
                new WebSocketHttpHeaders(),
                connectHeaders,
                new StompSessionHandlerAdapter() {
                }
        ).get(3, TimeUnit.SECONDS);

        session.subscribe("/topic/chat/1", new StompFrameHandler() {
            @Override
            public @NonNull Type getPayloadType(@NonNull StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(@NonNull StompHeaders headers, Object payload) {
                // no-op
            }
        });

        verify(projectAccessService, timeout(2000))
                .ensureProjectAccessible(1L, 1L);

        session.disconnect();
    }

    @Test
    @DisplayName("멤버 체크 실패 시 ERROR 프레임 수신 후 세션이 종료된다")
    void subscribeForbiddenReturnsStompErrorFrame() throws Exception {
        stompClient = createStompClient();
        String token = jwtTokenProvider.createAccessToken(1L, "test@itda.com", "USER");

        // 멤버 체크에서 FORBIDDEN을 발생시켜 ERROR 프레임 경로를 검증
        doThrow(new BusinessException(ErrorCode.FORBIDDEN))
                .when(projectAccessService)
                .ensureProjectAccessible(1L, 1L);

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + token);

        CompletableFuture<String> errorFuture = new CompletableFuture<>();
        StompSession session = stompClient.connectAsync(
                wsUrl(),
                new WebSocketHttpHeaders(),
                connectHeaders,
                new StompSessionHandlerAdapter() {
                    @Override
                    public void handleFrame(@NonNull StompHeaders headers, Object payload) {
                        errorFuture.complete(headers.getFirst("message"));
                    }
                }
        ).get(3, TimeUnit.SECONDS);

        try {
            session.subscribe("/topic/chat/1", new StompFrameHandler() {
                @Override
                public @NonNull Type getPayloadType(@NonNull StompHeaders headers) {
                    return byte[].class;
                }

                @Override
                public void handleFrame(@NonNull StompHeaders headers, Object payload) {
                    // no-op
                }
            });

            // Forbidden 시 ERROR 프레임이 오고, 이후 세션이 종료되는 것이 기대 동작
            String errorMessage = errorFuture.get(3, TimeUnit.SECONDS);
            assertThat(errorMessage).contains("FORBIDDEN");

            awaitSessionClosed(session);
            assertThat(session.isConnected()).isFalse();
        } finally {
            if (session.isConnected()) {
                session.disconnect();
            }
        }
    }

    private WebSocketStompClient createStompClient() {
        List<Transport> transports = List.of(new WebSocketTransport(new StandardWebSocketClient()));
        SockJsClient sockJsClient = new SockJsClient(transports);
        return new WebSocketStompClient(sockJsClient);
    }

    private String wsUrl() {
        return "http://localhost:" + port + "/ws";
    }

    @SuppressWarnings("BusyWait")
    private void awaitSessionClosed(StompSession session) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        while (session.isConnected() && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
    }
}
