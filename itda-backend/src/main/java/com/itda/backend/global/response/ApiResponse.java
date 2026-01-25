package com.itda.backend.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * 표준화된 API 응답 포맷
 * 
 * @param <T> 응답 데이터 타입
 * @param code 도메인 응답 코드 (SUCCESS, EMAIL_ALREADY_EXISTS 등)
 * @param message 사용자/로그용 요약 메시지 (선택). 사람이 읽기 쉬운 짧은 설명.
 * @param data 성공 시 응답 데이터 (선택).
 * @param details 실패 시 상세 정보 (선택). 클라이언트가 구조적으로 처리할 데이터
 *                (예: 필드별 검증 오류 맵)이며, 필요 없으면 생략한다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        String code,
        String message,
        T data,
        Object details
) {

    public static <T> ApiResponse<T> errorBody(ErrorCode errorCode, String message, Object details) {
        String resolvedMessage = resolveMessage(errorCode, message);
        return body(errorCode.getCode(), resolvedMessage, null, details);
    }

    // ===== ResponseEntity 래핑 (컨트롤러/예외 핸들러용) =====

    public static <T> ResponseEntity<ApiResponse<T>> success(T data) {
        return ResponseEntity.ok(body(StatusCode.SUCCESS.name(), null, data, null));
    }

    public static ResponseEntity<ApiResponse<Void>> success() {
        return ResponseEntity.ok(body(StatusCode.SUCCESS.name(), null, null, null));
    }

    public static <T> ResponseEntity<ApiResponse<T>> created(T data) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(body(StatusCode.SUCCESS.name(), "생성 완료", data, null));
    }
    public static <T> ResponseEntity<ApiResponse<T>> accepted(T data) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(body(StatusCode.ACCEPTED.name(), null, data, null));
    }


    public static ResponseEntity<ApiResponse<Void>> error(ErrorCode errorCode) {
        return ResponseEntity.status(HttpStatus.valueOf(errorCode.getStatus()))
                .body(errorBody(errorCode, null, null));
    }

    public static ResponseEntity<ApiResponse<Void>> error(ErrorCode errorCode, String message, Object details) {
        return ResponseEntity.status(HttpStatus.valueOf(errorCode.getStatus()))
                .body(errorBody(errorCode, message, details));
    }

    // ===== 내부 헬퍼 =====

    private static <T> ApiResponse<T> body(String code, String message, T data, Object details) {
        return new ApiResponse<>(
                code,
                message,
                data,
                details
        );
    }

    private static String resolveMessage(ErrorCode errorCode, String message) {
        return message != null ? message : errorCode.getMessage();
    }
}

