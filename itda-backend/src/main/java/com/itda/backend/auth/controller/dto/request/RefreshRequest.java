package com.itda.backend.auth.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 토큰 갱신 요청 DTO
 */
@Schema(description = "토큰 갱신 요청")
public record RefreshRequest(

        @Schema(description = "갱신 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        @NotBlank(message = "Refresh Token은 필수입니다")
        String refreshToken

) {}
