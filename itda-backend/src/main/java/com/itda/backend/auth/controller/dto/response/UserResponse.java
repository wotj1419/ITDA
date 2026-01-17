package com.itda.backend.auth.controller.dto.response;

import com.itda.backend.auth.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 사용자 정보 응답 DTO
 */
@Schema(description = "사용자 정보 응답")
public record UserResponse(

        @Schema(description = "사용자 ID", example = "1")
        Long id,

        @Schema(description = "사용자 이메일", example = "user@example.com")
        String email,

        @Schema(description = "사용자 이름", example = "홍길동")
        String name,

        @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
        String profileImageUrl,

        @Schema(description = "사용자 권한", example = "USER")
        String role,

        @Schema(description = "가입 일시", example = "2025-01-17T12:00:00")
        LocalDateTime createdAt

) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getProfileImageUrl(),
                user.getRole().name(),
                user.getCreatedAt()
        );
    }
}

