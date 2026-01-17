package com.itda.backend.auth.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자 도메인 엔티티
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private Long id;
    private String email;
    private String passwordHash;
    private String name;
    private String profileImageUrl;
    
    @Builder.Default
    private UserRole role = UserRole.USER;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 패스워드 해시 업데이트 (비밀번호 변경 시)
     */
    public void updatePasswordHash(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    /**
     * 프로필 정보 업데이트
     */
    public void updateProfile(String name, String profileImageUrl) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (profileImageUrl != null) {
            this.profileImageUrl = profileImageUrl;
        }
    }
}
