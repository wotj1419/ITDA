package com.itda.backend.collab.config;

import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import com.itda.backend.global.security.CustomUserDetails;
import com.itda.backend.global.security.JwtTokenProvider;
import com.itda.backend.project.service.ProjectAccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String DEFAULT_ROLE = "USER";

    private static final List<Pattern> PROJECT_ID_PATTERNS = List.of(
            Pattern.compile("^/pub/chat/(\\d+)/?$"),
            Pattern.compile("^/topic/chat/(\\d+)/?$"),
            Pattern.compile("^/pub/presence/(\\d+)/?$"),
            Pattern.compile("^/topic/presence/(\\d+)/?$"),
            Pattern.compile("^/pub/rtc/(\\d+)/?$"),
            Pattern.compile("^/topic/projects/(\\d+)/?$")
    );

    private final JwtTokenProvider jwtTokenProvider;
    private final ProjectAccessService projectAccessService;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();
        if (command == StompCommand.CONNECT) {
            authenticate(accessor);
            return message;
        }

        if (command == StompCommand.SUBSCRIBE || command == StompCommand.SEND) {
            authorize(accessor);
        }

        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER);
        String token = jwtTokenProvider.extractToken(authHeader);
        if (token == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        JwtTokenProvider.AccessTokenPayload payload = jwtTokenProvider.parseAccessToken(token);
        if (payload.userId() == null || payload.email() == null) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        String role = payload.role() != null ? payload.role() : DEFAULT_ROLE;
        CustomUserDetails userDetails = new CustomUserDetails(payload.userId(), payload.email(), role);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        accessor.setUser(authentication);
    }

    private void authorize(StompHeaderAccessor accessor) {
        Authentication authentication = getAuthentication(accessor.getUser());
        CustomUserDetails userDetails = getUserDetails(authentication);

        String destination = accessor.getDestination();
        Long projectId = extractProjectId(destination);
        if (projectId == null) {
            return;
        }

        projectAccessService.ensureProjectAccessible(projectId, userDetails.getUserId());
    }

    private Authentication getAuthentication(Principal principal) {
        if (principal instanceof Authentication authentication) {
            return authentication;
        }
        throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }

    private CustomUserDetails getUserDetails(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails userDetails) {
            return userDetails;
        }
        throw new BusinessException(ErrorCode.INVALID_TOKEN);
    }

    private Long extractProjectId(String destination) {
        if (destination == null || destination.isBlank()) {
            return null;
        }
        for (Pattern pattern : PROJECT_ID_PATTERNS) {
            Matcher matcher = pattern.matcher(destination);
            if (matcher.matches()) {
                try {
                    return Long.parseLong(matcher.group(1));
                } catch (NumberFormatException e) {
                    log.warn("Invalid projectId in destination: {}", destination, e);
                    throw new BusinessException(ErrorCode.INVALID_REQUEST);
                }
            }
        }
        return null;
    }
}
