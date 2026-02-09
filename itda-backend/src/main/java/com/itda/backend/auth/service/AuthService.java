package com.itda.backend.auth.service;

import com.itda.backend.auth.controller.dto.request.LoginRequest;
import com.itda.backend.auth.controller.dto.response.LoginResponse;
import com.itda.backend.auth.controller.dto.request.SignupRequest;
import com.itda.backend.auth.controller.dto.response.UserResponse;
import com.itda.backend.auth.domain.User;
import com.itda.backend.auth.domain.UserRole;
import com.itda.backend.auth.repository.RefreshTokenRepository;
import com.itda.backend.auth.repository.UserMapper;
import com.itda.backend.asset.domain.Asset;
import com.itda.backend.asset.repository.AssetMapper;
import com.itda.backend.auth.storage.ProfileImageCleaner;
import com.itda.backend.auth.storage.ProfileImageStorage;
import com.itda.backend.auth.storage.ProfileImageStorageResult;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;
import java.util.Set;
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
    private final ProfileImageStorage profileImageStorage;
    private final ProfileImageAssetRegistrar profileImageAssetRegistrar;
    private final ProfileImageCleaner profileImageCleaner;
    private final AssetMapper assetMapper;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );

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
     * 비밀번호 재설정 요청
     */
    @Transactional(readOnly = true)
    public void requestPasswordReset(String email) {
        User user = getUserByEmailOrThrow(email);
        log.info("Password reset requested: {}", user.getEmail());
    }

    /**
     * 비밀번호 재설정 완료
     */
    @Transactional
    public void confirmPasswordReset(String email, String newPassword) {
        User user = getUserByEmailOrThrow(email);
        String encoded = passwordEncoder.encode(newPassword);
        userMapper.updatePassword(user.getId(), encoded);
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

    /**
     * Profile image upload
     */
    @Transactional
    public UserResponse updateProfileImage(Long userId, MultipartFile file) {
        validateImageFile(file);

        User user = getUserByIdOrThrow(userId);
        String previousProfileUrl = user.getProfileImageUrl();

        ProfileImageStorageResult storedImage = storeProfileImageOrThrow(userId, file);
        Long assetId = null;
        try {
            assetId = profileImageAssetRegistrar.registerProfileImage(
                    userId,
                    storedImage.storageKey(),
                    storedImage.sizeBytes(),
                    storedImage.contentType(),
                    storedImage.storageProvider()
            );
            String profileImageUrl = buildProfileImageUrl(assetId);
            ensureProfileUpdatedOrThrow(userId, null, profileImageUrl);
        } catch (RuntimeException e) {
            cleanupStoredAsset(assetId, storedImage);
            throw e;
        }

        cleanupPreviousProfileImage(userId, previousProfileUrl, assetId);
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

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "image file is required");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(normalizeContentType(contentType))) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "unsupported image content type");
        }
        String filename = file.getOriginalFilename();
        if (filename != null && !filename.isBlank() && !hasAllowedExtension(filename)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "unsupported image file extension");
        }
    }

    private boolean hasAllowedExtension(String filename) {
        String lower = filename.toLowerCase();
        return lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")
                || lower.endsWith(".png")
                || lower.endsWith(".webp");
    }

    private String normalizeContentType(String contentType) {
        String trimmed = contentType.trim().toLowerCase();
        int semicolon = trimmed.indexOf(';');
        return semicolon > 0 ? trimmed.substring(0, semicolon).trim() : trimmed;
    }

    private ProfileImageStorageResult storeProfileImageOrThrow(Long userId, MultipartFile file) {
        try {
            return profileImageStorage.save(userId, file.getBytes(), file.getContentType());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, e);
        }
    }

    private String buildProfileImageUrl(Long assetId) {
        if (assetId == null) {
            return null;
        }
        return "/api/profile-images/" + assetId;
    }

    private void cleanupStoredAsset(Long assetId, ProfileImageStorageResult storedImage) {
        if (assetId != null) {
            try {
                assetMapper.deleteById(assetId);
            } catch (Exception e) {
                log.warn("[AuthService] Failed to rollback profile asset record: assetId={}", assetId, e);
            }
        }
        if (storedImage != null) {
            profileImageCleaner.delete(storedImage.storageProvider(), storedImage.storageKey());
        }
    }

    private void cleanupPreviousProfileImage(Long userId, String previousUrl, Long newAssetId) {
        Long assetId = extractProfileImageAssetId(previousUrl);
        if (assetId == null || assetId.equals(newAssetId)) {
            return;
        }
        Asset asset = assetMapper.findById(assetId).orElse(null);
        if (asset == null || asset.getProjectId() != null) {
            return;
        }
        if (!Objects.equals(asset.getOwnerId(), userId)) {
            return;
        }
        try {
            assetMapper.deleteById(assetId);
        } catch (Exception e) {
            log.warn("[AuthService] Failed to delete profile asset record: assetId={}", assetId, e);
        }
        profileImageCleaner.delete(asset.getStorageProvider(), asset.getStorageKey());
    }

    private Long extractProfileImageAssetId(String profileImageUrl) {
        if (profileImageUrl == null) {
            return null;
        }
        String marker = "/api/profile-images/";
        String trimmed = profileImageUrl.trim();
        int index = trimmed.indexOf(marker);
        if (index < 0) {
            return null;
        }
        String tail = trimmed.substring(index + marker.length());
        int queryIndex = tail.indexOf('?');
        if (queryIndex >= 0) {
            tail = tail.substring(0, queryIndex);
        }
        int slashIndex = tail.indexOf('/');
        if (slashIndex >= 0) {
            tail = tail.substring(0, slashIndex);
        }
        if (tail.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(tail);
        } catch (NumberFormatException e) {
            return null;
        }
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
