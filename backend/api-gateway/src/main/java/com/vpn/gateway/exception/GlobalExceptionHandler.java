package com.vpn.gateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vpn.common.constant.ErrorCode;
import com.vpn.common.dto.ErrorResponse;
import com.vpn.common.util.StringUtils;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


@Slf4j
@Component
@Order(-1)
@RequiredArgsConstructor
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(@NonNull ServerWebExchange exchange, @NonNull Throwable ex) {
        log.error("Gateway error occurred: Path: {}, Error: {}",
                exchange.getRequest().getPath(), ex.getMessage());
        String traceId = StringUtils.generateUuid();

        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
        ErrorCode errorCode = ErrorCode.SERVER_UNAVAILABLE;

        //todo проверка типовых ошибок 404, 401 и тд.
        if (ex instanceof org.springframework.web.server.ResponseStatusException responseStatusException) {
            status = HttpStatus.valueOf(responseStatusException.getStatusCode().value());
            if (status == HttpStatus.NOT_FOUND) {
                errorCode = ErrorCode.INVALID_REQUEST;
            }
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(errorCode.getCode())
                .message("Gateway Error: " + ex.getMessage())
                .traceId(traceId)
                .build();

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("Error writing response", e);
            return exchange.getResponse().setComplete();
        }
    }
}
