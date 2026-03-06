package com.vpn.gateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vpn.common.constant.ErrorCode;
import com.vpn.common.dto.ApiResponse;
import com.vpn.common.dto.ErrorResponse;
import com.vpn.common.util.StringUtils;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


/**
 * Global Exception Handler для API Gateway
 * Обрабатывает все неперехваченные исключения
 */
@Slf4j
@Component
@Order(-1)
@RequiredArgsConstructor
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(@NonNull ServerWebExchange exchange, @NonNull Throwable ex) {
        String path = exchange.getRequest().getPath().value();
        String traceId = StringUtils.generateUuid();

        HttpStatus status;
        ErrorCode errorCode;
        String message;

        if (ex instanceof NotFoundException) {
            status = HttpStatus.NOT_FOUND;
            errorCode = ErrorCode.INVALID_REQUEST;
            message = "Service not found or unavailable";
            log.warn("[TraceID: {}] Service not found: Path: {}", traceId, path);

        } else if (ex instanceof ResponseStatusException rse) {
            status = HttpStatus.valueOf(rse.getStatusCode().value());

            switch (status) {
                case NOT_FOUND -> {
                    errorCode = ErrorCode.INVALID_REQUEST;
                    message = "Resource not found";
                }
                case UNAUTHORIZED -> {
                    errorCode = ErrorCode.UNAUTHORIZED;
                    message = "Authentication required";
                }
                case FORBIDDEN -> {
                    errorCode = ErrorCode.FORBIDDEN;
                    message = "Access denied";
                }
                case TOO_MANY_REQUESTS -> {
                    errorCode = ErrorCode.RATE_LIMIT_EXCEEDED;
                    message = "Too many requests. Please try again later";
                }
                case SERVICE_UNAVAILABLE -> {
                    errorCode = ErrorCode.SERVER_UNAVAILABLE;
                    message = "Service temporarily unavailable";
                }
                case GATEWAY_TIMEOUT -> {
                    errorCode = ErrorCode.SERVER_UNAVAILABLE;
                    message = "Gateway timeout";
                }
                default -> {
                    errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
                    message = rse.getReason() != null ? rse.getReason() : "Internal error";
                }
            }

            log.warn("[TraceID: {}] Response status exception: {}, Path: {}",
                    traceId, status, path);

        } else if (ex instanceof IllegalArgumentException) {
            status = HttpStatus.BAD_REQUEST;
            errorCode = ErrorCode.INVALID_REQUEST;
            message = ex.getMessage();
            log.warn("[TraceID: {}] Invalid argument: {}, Path: {}",
                    traceId, message, path);

        } else if (ex.getCause() instanceof java.net.ConnectException) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            errorCode = ErrorCode.SERVER_UNAVAILABLE;
            message = "Unable to connect to downstream service";
            log.error("[TraceID: {}] Connection error: Path: {}, Error: {}",
                    traceId, path, ex.getMessage());

        } else if (ex.getCause() instanceof java.util.concurrent.TimeoutException) {
            status = HttpStatus.GATEWAY_TIMEOUT;
            errorCode = ErrorCode.SERVER_UNAVAILABLE;
            message = "Request timeout";
            log.error("[TraceID: {}] Timeout: Path: {}", traceId, path);

        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
            message = "An unexpected error occurred";
            log.error("[TraceID: {}] Unexpected error: Path: {}",
                    traceId, path, ex);
        }

        return writeErrorResponse(exchange, status, errorCode, message, traceId);
    }

    private Mono<Void> writeErrorResponse(
            ServerWebExchange exchange,
            HttpStatus status,
            ErrorCode errorCode,
            String message,
            String traceId) {

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(message)
                .traceId(traceId)
                .build();

        ApiResponse<Void> apiResponse = ApiResponse.error(errorResponse);

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().set("X-Trace-Id", traceId);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(apiResponse);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("[TraceID: {}] Error serializing error response", traceId, e);
            return exchange.getResponse().setComplete();
        }
    }
}
