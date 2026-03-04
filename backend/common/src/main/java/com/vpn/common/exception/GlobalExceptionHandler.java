package com.vpn.common.exception;

import com.vpn.common.constant.ErrorCode;
import com.vpn.common.dto.ApiResponse;
import com.vpn.common.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@RestControllerAdvice
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(BaseException ex) {
        log.warn("Business error [{}]: {}", ex.getErrorCode().getCode(), ex.getMessage());
        return buildErrorResponse(ex.getErrorCode(), ex.getMessage(), HttpStatus.BAD_REQUEST, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Validation error: {}", errors);
        return buildErrorResponse(ErrorCode.INVALID_REQUEST, "Validation failed", HttpStatus.BAD_REQUEST, errors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        String message = String.format("Parameter '%s' should be of type %s", ex.getName(), Objects.requireNonNull(ex.getRequiredType()).getSimpleName());
        log.warn("Type mismatch: {}", message);
        return buildErrorResponse(ErrorCode.INVALID_REQUEST, message, HttpStatus.BAD_REQUEST, null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.warn("Malformed JSON request: {}", ex.getMessage());
        return buildErrorResponse(ErrorCode.INVALID_REQUEST, "Malformed JSON request", HttpStatus.BAD_REQUEST, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
        log.error("Unhandled internal error: ", ex);
        return buildErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_SERVER_ERROR.getDefaultMessage(), HttpStatus.INTERNAL_SERVER_ERROR, null);
    }

    private ResponseEntity<ApiResponse<Void>> buildErrorResponse(ErrorCode code,
                                                                 String message,
                                                                 HttpStatus status,
                                                                 Map<String, String> validationErrors) {
        ErrorResponse response = ErrorResponse.builder()
                .code(code.getCode())
                .message(message)
                .validationErrors(validationErrors)
                .build();

        return ResponseEntity.status(status).body(ApiResponse.error(response));

    }
}
