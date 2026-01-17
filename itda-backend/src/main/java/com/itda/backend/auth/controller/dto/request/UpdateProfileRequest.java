package com.itda.backend.auth.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

/**
 * 프로필 수정 요청 DTO
 */
@Schema(description = "프로필 수정 요청")
public record UpdateProfileRequest(

        @Schema(description = "변경할 이름", example = "김철수")
        @Size(min = 2, max = 100, message = "이름은 2자 이상 100자 이하여야 합니다")
        String name,

        @Schema(description = "변경할 프로필 이미지 URL", example = "https://example.com/new-profile.jpg")
        @Size(max = 500, message = "프로필 이미지 URL은 500자 이하여야 합니다")
        String profileImageUrl

) {
    @AssertTrue(message = "수정할 값이 없습니다")
    public boolean isAnyFieldPresent() {
        return name != null || profileImageUrl != null;
    }
}

