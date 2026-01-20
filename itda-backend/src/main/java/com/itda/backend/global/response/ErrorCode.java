package com.itda.backend.global.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 에러 코드 enum
 * HTTP 상태 코드와 메시지를 함께 관리
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ===== 400 Bad Request =====
    INVALID_REQUEST(400, "요청 파라미터가 올바르지 않습니다"),
    INVALID_INPUT_VALUE(400, "잘못된 입력 값입니다"),

    // ===== 401 Unauthorized =====
    UNAUTHORIZED(401, "인증이 필요합니다"),
    INVALID_TOKEN(401, "유효하지 않은 토큰입니다"),
    EXPIRED_TOKEN(401, "만료된 토큰입니다"),
    INVALID_PASSWORD(401, "비밀번호가 일치하지 않습니다"),

    // ===== 403 Forbidden =====
    FORBIDDEN(403, "권한이 없습니다"),

    // ===== 404 Not Found =====
    USER_NOT_FOUND(404, "사용자를 찾을 수 없습니다"),
    PROJECT_NOT_FOUND(404, "프로젝트를 찾을 수 없습니다"),
    SCENE_NOT_FOUND(404, "씬을 찾을 수 없습니다"),
    NODE_NOT_FOUND(404, "노드를 찾을 수 없습니다"),
    JOB_NOT_FOUND(404, "작업을 찾을 수 없습니다"),

    // ===== 409 Conflict =====
    JOB_ALREADY_RUNNING(409, "이미 실행 중인 작업입니다"),
    EMAIL_ALREADY_EXISTS(409, "이미 가입된 이메일입니다"),
    SCENE_LIMIT_EXCEEDED(409, "씬 개수 제한을 초과했습니다"),

    // ===== 500 Internal Server Error =====
    JOB_EXECUTION_FAILED(500, "작업 실행 중 오류가 발생했습니다"),
    MERGE_FAILED(500, "씬 병합에 실패했습니다"),
    INTERNAL_ERROR(500, "서버 내부 오류가 발생했습니다");

    // 추후 도메인 확장 시 추가 예정

    private final int status;
    private final String message;

    public String getCode() {
        return name();
    }
}
