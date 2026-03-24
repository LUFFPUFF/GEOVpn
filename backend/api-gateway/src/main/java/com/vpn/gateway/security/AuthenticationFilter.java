package com.vpn.gateway.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vpn.common.constant.ErrorCode;
import com.vpn.common.dto.ApiResponse;
import com.vpn.common.dto.ErrorResponse;
import com.vpn.gateway.util.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


/**
 * JWT Authentication Filter для API Gateway
 * Проверяет JWT токен и добавляет X-User-Id header для downstream сервисов
 */

@Slf4j
@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final RouteValidator validator;
    private final ObjectMapper objectMapper;

    public AuthenticationFilter(RouteValidator validator, ObjectMapper objectMapper) {
        super(Config.class);
        this.validator = validator;
        this.objectMapper = objectMapper;
    }

    public static class Config {}

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            if (request.getMethod() == HttpMethod.OPTIONS) {
                return chain.filter(exchange);
            }

            if (validator.isSecured(request)) {
                log.debug("Checking header X-User-Id for path: {}", request.getPath());

                if (!request.getHeaders().containsKey("X-User-Id")) {
                    log.warn("401: Missing X-User-Id for secured path {}", request.getPath());
                    return onError(exchange, "X-User-Id header is missing", HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED);
                }

            }

            return chain.filter(exchange);
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status, ErrorCode errorCode) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);

        response.getHeaders().add("Access-Control-Allow-Origin", "http://localhost:5173");
        response.getHeaders().add("Access-Control-Allow-Methods", "*");
        response.getHeaders().add("Access-Control-Allow-Headers", "*");
        response.getHeaders().add("Access-Control-Allow-Credentials", "true");

        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(message)
                .build();

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(ApiResponse.error(errorResponse));
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            return response.setComplete();
        }
    }
}
