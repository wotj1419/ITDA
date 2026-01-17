package com.itda.backend.auth.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Redis 기반 Refresh Token 저장소
 * Key: "refresh_token:{userId}"
 * Value: refreshToken
 */
@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {

    private static final String KEY_PREFIX = "refresh_token:";
    private static final DefaultRedisScript<Long> ROTATE_IF_MATCH_SCRIPT =
            new DefaultRedisScript<>(
                    "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "redis.call('set', KEYS[1], ARGV[2], 'px', ARGV[3]); " +
                    "return 1 else return 0 end",
                    Long.class
            );

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Refresh Token 저장
     * @param userId 사용자 ID
     * @param refreshToken Refresh Token
     * @param ttlMillis TTL (밀리초)
     */
    public void save(Long userId, String refreshToken, long ttlMillis) {
        validateTtlMillis(ttlMillis);
        requireToken(refreshToken, "refreshToken");
        String key = buildKey(userId);
        redisTemplate.opsForValue().set(key, refreshToken, ttlMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Refresh Token 조회
     * @param userId 사용자 ID
     * @return Refresh Token (없으면 null)
     */
    public String findByUserId(Long userId) {
        String key = buildKey(userId);
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * Refresh Token 삭제 (로그아웃)
     * @param userId 사용자 ID
     */
    public void deleteByUserId(Long userId) {
        String key = buildKey(userId);
        redisTemplate.delete(key);
    }

    /**
     * Refresh Token 존재 여부 확인
     * @param userId 사용자 ID
     * @return 존재 여부
     */
    public boolean existsByUserId(Long userId) {
        String key = buildKey(userId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 저장된 Refresh Token이 일치할 때만 새 토큰으로 교체
     * @param userId 사용자 ID
     * @param expectedToken 기존 Refresh Token
     * @param newToken 새 Refresh Token
     * @param ttlMillis TTL (밀리초)
     * @return 교체 성공 여부
     */
    public boolean rotateIfMatch(Long userId, String expectedToken, String newToken, long ttlMillis) {
        validateTtlMillis(ttlMillis);
        requireToken(expectedToken, "expectedToken");
        requireToken(newToken, "newToken");
        String key = buildKey(userId);
        Long result = redisTemplate.execute(
                ROTATE_IF_MATCH_SCRIPT,
                Collections.singletonList(key),
                expectedToken,
                newToken,
                String.valueOf(ttlMillis)
        );
        if (result == null) {
            throw new IllegalStateException("Redis script execution returned null");
        }
        return Long.valueOf(1L).equals(result);
    }

    private String buildKey(Long userId) {
        Objects.requireNonNull(userId, "userId");
        return KEY_PREFIX + userId;
    }

    private void validateTtlMillis(long ttlMillis) {
        if (ttlMillis <= 0) {
            throw new IllegalArgumentException("ttlMillis must be positive");
        }
    }

    private void requireToken(String token, String paramName) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException(paramName + " must not be blank");
        }
    }
}
