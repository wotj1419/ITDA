package com.itda.backend.auth.service;

import com.itda.backend.auth.controller.dto.request.LoginRequest;
import com.itda.backend.auth.controller.dto.response.LoginResponse;
import com.itda.backend.auth.controller.dto.request.SignupRequest;
import com.itda.backend.auth.controller.dto.response.UserResponse;
import com.itda.backend.auth.domain.User;
import com.itda.backend.auth.domain.UserRole;
import com.itda.backend.auth.repository.RefreshTokenRepository;
import com.itda.backend.auth.repository.UserMapper;
import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.exception.UnauthorizedException;
import com.itda.backend.global.response.ErrorCode;
import com.itda.backend.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 서비스
 * 회원가입, 로그인, 토큰 갱신, 로그아웃 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 회원가입
     */
    @Transactional
    public UserResponse signup(SignupRequest request) {
        User user = buildUser(request);
        saveUserOrThrowDuplicate(user);
        log.info("New user registered: {}", user.getEmail());
        return UserResponse.from(user);
    }

    /**
     * 로그인
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = getUserByEmailOrThrow(request.email());
        validatePasswordOrThrow(request.password(), user.getPasswordHash());
        return issueTokens(user, null);
    }

    /**
     * 토큰 갱신 (Refresh Token Rotation)
     */
    @Transactional
    public LoginResponse refresh(String refreshToken) {
        validateRefreshTokenOrThrow(refreshToken);
        Long userId = jwtTokenProvider.getUserIdFromRefreshToken(refreshToken);
        User user = getUserByIdOrThrow(userId);
        return issueTokens(user, refreshToken);
    }

    /**
     * 로그아웃
     */
    @Transactional
    public void logout(Long userId) {
        // Redis에서 Refresh Token 삭제
        refreshTokenRepository.deleteByUserId(userId);
        log.info("User logged out: userId={}", userId);
    }

    /**
     * 내 정보 조회
     */
    @Transactional(readOnly = true)
    public UserResponse getMe(Long userId) {
        User user = getUserByIdOrThrow(userId);
        return UserResponse.from(user);
    }

    /**
     * 프로필 수정
     */
    @Transactional
    public UserResponse updateProfile(Long userId, String name, String profileImageUrl) {
        ensureProfileUpdatedOrThrow(userId, name, profileImageUrl);
        log.info("Profile updated: userId={}", userId);
        User updatedUser = getUserByIdOrThrow(userId);
        return UserResponse.from(updatedUser);
    }

    // ===== Private Methods =====

    /**
     * Access Token + Refresh Token 발급 후 Redis 저장
     */
    private LoginResponse issueTokens(User user, String previousRefreshToken) {
        String accessToken = jwtTokenProvider.createAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getEmail());
        long refreshTokenTtlMillis = jwtTokenProvider.getRefreshTokenExpiration();

        saveOrRotateRefreshToken(user.getId(), previousRefreshToken, refreshToken, refreshTokenTtlMillis);

        return new LoginResponse(
                accessToken,
                refreshToken,
                jwtTokenProvider.getAccessTokenExpiration() / 1000 // 초 단위
        );
    }

    private User buildUser(SignupRequest request) {
        return User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .name(request.name())
                .role(UserRole.USER)
                .build();
    }

    private void saveUserOrThrowDuplicate(User user) {
        try {
            userMapper.insertUser(user);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
    }

    private User getUserByEmailOrThrow(String email) {
        return userMapper.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private void validatePasswordOrThrow(String rawPassword, String passwordHash) {
        if (!passwordEncoder.matches(rawPassword, passwordHash)) {
            throw new UnauthorizedException(ErrorCode.INVALID_PASSWORD);
        }
    }

    private void validateRefreshTokenOrThrow(String refreshToken) {
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new UnauthorizedException(ErrorCode.INVALID_TOKEN);
        }
    }

    private void ensureProfileUpdatedOrThrow(Long userId, String name, String profileImageUrl) {
        int updated = userMapper.updateProfile(userId, name, profileImageUrl);
        if (updated != 0) {
            return;
        }
        throw new BusinessException(ErrorCode.USER_NOT_FOUND);
    }

    private void saveOrRotateRefreshToken(
            Long userId,
            String previousRefreshToken,
            String newRefreshToken,
            long refreshTokenTtlMillis) {
        if (previousRefreshToken == null) {
            refreshTokenRepository.save(userId, newRefreshToken, refreshTokenTtlMillis);
            return;
        }
        boolean rotated = refreshTokenRepository.rotateIfMatch(
                userId,
                previousRefreshToken,
                newRefreshToken,
                refreshTokenTtlMillis);
        if (!rotated) {
            throw new UnauthorizedException(ErrorCode.INVALID_TOKEN);
        }
    }

    private User getUserByIdOrThrow(Long userId) {
        return userMapper.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
