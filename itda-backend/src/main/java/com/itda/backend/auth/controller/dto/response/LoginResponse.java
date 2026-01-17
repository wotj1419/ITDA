package com.itda.backend.auth.controller.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 로그인 응답 DTO
 */
@Schema(description = "로그인 응답")
public record LoginResponse(

        @Schema(description = "액세스 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String accessToken,

        @Schema(description = "갱신 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String refreshToken,

        @Schema(description = "액세스 토큰 만료 시간 (초)", example = "3600")
        Long expiresIn

) {}
