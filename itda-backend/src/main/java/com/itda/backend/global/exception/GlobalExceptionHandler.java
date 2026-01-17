package com.itda.backend.global.exception;

import com.itda.backend.global.response.ApiResponse;
import com.itda.backend.global.response.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

/**
 * 전역 예외 처리기
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Validation 예외 처리 (@Valid 실패)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException e) {
        List<ValidationError> errors = buildValidationErrors(e);
        log.warn("Validation error: {}", errors);
        return ApiResponse.error(ErrorCode.INVALID_INPUT_VALUE, null, errors);
    }

    /**
     * 인증 예외 처리
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorizedException(UnauthorizedException e) {
        log.warn("Unauthorized: {}", e.getMessage());
        return ApiResponse.error(e.getErrorCode(), null, null);
    }

    /**
     * 비즈니스 예외 처리
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        log.warn("Business exception: {}", e.getMessage());
        return ApiResponse.error(e.getErrorCode(), null, null);
    }

    /**
     * IllegalArgumentException 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("IllegalArgument: {}", e.getMessage());
        return ApiResponse.error(ErrorCode.INVALID_REQUEST, null, null);
    }

    /**
     * 일반 예외 처리 (최종 폴백)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unexpected error", e);
        return ApiResponse.error(ErrorCode.INTERNAL_ERROR);
    }

    private List<ValidationError> buildValidationErrors(MethodArgumentNotValidException e) {
        List<ValidationError> errors = new ArrayList<>();
        e.getBindingResult().getFieldErrors().forEach(error ->
                errors.add(new ValidationError(error.getField(), error.getDefaultMessage()))
        );
        e.getBindingResult().getGlobalErrors().forEach(error ->
                errors.add(new ValidationError(error.getObjectName(), error.getDefaultMessage()))
        );
        return errors;
    }

    private record ValidationError(String field, String message) {
    }
}
