package com.itda.backend.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itda.backend.global.response.ApiResponse;
import com.itda.backend.global.response.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 인증 필터
 * 모든 요청에서 JWT 토큰을 검증하고 SecurityContext에 인증 정보 설정
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String DEFAULT_ROLE = "USER";

    // 인증 제외 경로
    private static final List<String> EXCLUDE_PATHS = List.of(
            "/api/auth/signup",
            "/api/auth/login",
            "/api/auth/refresh",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/ws",
            "/ws/**",
            "/error",
            "/health",
            "/test/**",
            "/actuator/**"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        String token = jwtTokenProvider.extractToken(authHeader);

        if (token == null) {
            sendError(response, ErrorCode.UNAUTHORIZED);
            return;
        }

        try {
            JwtTokenProvider.AccessTokenPayload payload = jwtTokenProvider.parseAccessToken(token);
            if (payload.userId() == null || payload.email() == null) {
                sendError(response, ErrorCode.INVALID_TOKEN);
                return;
            }
            String role = payload.role() != null ? payload.role() : DEFAULT_ROLE;

            // CustomUserDetails 생성 (role은 토큰에서 가져오거나 DB 조회 필요 시 수정)
            CustomUserDetails userDetails = new CustomUserDetails(payload.userId(), payload.email(), role);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("JWT 인증 처리 중 오류 발생", e);
            sendError(response, ErrorCode.INVALID_TOKEN);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        return isExcludedPath(request.getRequestURI());
    }

    private boolean isExcludedPath(String requestUri) {
        return EXCLUDE_PATHS.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, requestUri));
    }

    private void sendError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Void> apiResponse = ApiResponse.errorBody(errorCode, null, null);
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
