package com.itda.backend.global.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * JWT 토큰 생성 및 검증 컴포넌트
 * Access Token과 Refresh Token을 모두 관리
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private static final String ACCESS_TOKEN_SUBJECT = "AccessToken";
    private static final String REFRESH_TOKEN_SUBJECT = "RefreshToken";
    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";
    private static final String BEARER_PREFIX = "Bearer ";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-expiration}")
    private Long accessTokenExpiration;

    @Value("${jwt.refresh-expiration}")
    private Long refreshTokenExpiration;

    private Algorithm algorithm;
    private JWTVerifier accessTokenVerifier;
    private JWTVerifier refreshTokenVerifier;

    @PostConstruct
    void initVerifier() {
        algorithm = Algorithm.HMAC512(secret);
        accessTokenVerifier = JWT.require(algorithm)
                .withSubject(ACCESS_TOKEN_SUBJECT)
                .build();
        refreshTokenVerifier = JWT.require(algorithm)
                .withSubject(REFRESH_TOKEN_SUBJECT)
                .build();
    }

    /**
     * Access Token 생성 (1일)
     */
    public String createAccessToken(Long userId, String email, String role) {
        Date now = new Date();
        return JWT.create()
                .withSubject(ACCESS_TOKEN_SUBJECT)
                .withExpiresAt(new Date(now.getTime() + accessTokenExpiration))
                .withClaim(CLAIM_USER_ID, userId)
                .withClaim(CLAIM_EMAIL, email)
                .withClaim(CLAIM_ROLE, role)
                .sign(algorithm);
    }

    /**
     * Refresh Token 생성 (7일)
     */
    public String createRefreshToken(Long userId, String email) {
        Date now = new Date();
        return JWT.create()
                .withSubject(REFRESH_TOKEN_SUBJECT)
                .withExpiresAt(new Date(now.getTime() + refreshTokenExpiration))
                .withClaim(CLAIM_USER_ID, userId)
                .withClaim(CLAIM_EMAIL, email)
                .sign(algorithm);
    }

    /**
     * Access Token 유효성 검증
     */
    public boolean validateAccessToken(String token) {
        return validateToken(token, ACCESS_TOKEN_SUBJECT);
    }

    /**
     * Refresh Token 유효성 검증
     */
    public boolean validateRefreshToken(String token) {
        return validateToken(token, REFRESH_TOKEN_SUBJECT);
    }

    /**
     * Access Token에서 사용자 정보 추출
     */
    public AccessTokenPayload parseAccessToken(String token) {
        DecodedJWT decodedJWT = verifyToken(token, ACCESS_TOKEN_SUBJECT);
        return new AccessTokenPayload(
                decodedJWT.getClaim(CLAIM_USER_ID).asLong(),
                decodedJWT.getClaim(CLAIM_EMAIL).asString(),
                decodedJWT.getClaim(CLAIM_ROLE).asString()
        );
    }

    /**
     * Refresh Token에서 User ID 추출
     */
    public Long getUserIdFromRefreshToken(String token) {
        DecodedJWT decodedJWT = verifyToken(token, REFRESH_TOKEN_SUBJECT);
        return decodedJWT.getClaim(CLAIM_USER_ID).asLong();
    }

    /**
     * Authorization 헤더에서 토큰 추출
     * "Bearer " 접두사 제거
     */
    public String extractToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }

        String value = authorizationHeader.trim();

        // Bearer 접두사 처리 (대소문자 무관)
        if (value.toLowerCase().startsWith(BEARER_PREFIX.toLowerCase())) {
            value = value.substring(BEARER_PREFIX.length()).trim();
        }

        // 중복 Bearer 처리 (방어 코드)
        if (value.toLowerCase().startsWith(BEARER_PREFIX.toLowerCase())) {
            value = value.substring(BEARER_PREFIX.length()).trim();
        }

        return value.isEmpty() ? null : value;
    }

    /**
     * Access Token TTL(ms) 반환
     */
    public Long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    /**
     * Refresh Token TTL(ms) 반환
     */
    public Long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    // ===== Private Methods =====

    private boolean validateToken(String token, String expectedSubject) {
        try {
            verifyToken(token, expectedSubject);
            return true;
        } catch (JWTVerificationException e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    private DecodedJWT verifyToken(String token, String expectedSubject) throws JWTVerificationException {
        if (ACCESS_TOKEN_SUBJECT.equals(expectedSubject)) {
            return accessTokenVerifier.verify(token);
        }
        if (REFRESH_TOKEN_SUBJECT.equals(expectedSubject)) {
            return refreshTokenVerifier.verify(token);
        }
        return JWT.require(algorithm)
                .withSubject(expectedSubject)
                .build()
                .verify(token);
    }

    public record AccessTokenPayload(Long userId, String email, String role) {
    }
}
