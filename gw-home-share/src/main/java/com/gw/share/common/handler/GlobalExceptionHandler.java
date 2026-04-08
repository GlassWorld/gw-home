package com.gw.share.common.handler;

import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException exception,
            HttpServletRequest request
    ) {
        ErrorCode errorCode = exception.getErrorCode();
        log.warn(
                "비즈니스 예외 발생 - method: {}, path: {}, errorCode: {}, message: {}, detailMessage: {}",
                request.getMethod(),
                request.getRequestURI(),
                errorCode.name(),
                exception.getMessage(),
                exception.getDetailMessage()
        );
        String responseMessage = exception.getDetailMessage() != null && !exception.getDetailMessage().isBlank()
                ? exception.getDetailMessage()
                : exception.getMessage();
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(responseMessage));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : ErrorCode.BAD_REQUEST.getMessage();
        String fieldName = fieldError != null ? fieldError.getField() : "unknown";

        log.warn(
                "요청 검증 예외 발생 - method: {}, path: {}, field: {}, message: {}",
                request.getMethod(),
                request.getRequestURI(),
                fieldName,
                message
        );

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.fail(message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error(
                "처리되지 않은 예외 발생 - method: {}, path: {}",
                request.getMethod(),
                request.getRequestURI(),
                exception
        );
        return ResponseEntity
                .status(ErrorCode.INTERNAL_ERROR.getStatus())
                .body(ApiResponse.fail(ErrorCode.INTERNAL_ERROR.getMessage()));
    }
}
