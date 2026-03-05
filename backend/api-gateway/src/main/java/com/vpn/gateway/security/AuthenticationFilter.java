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
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config>  {

    private final RouteValidator validator;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    public AuthenticationFilter(RouteValidator validator, JwtUtil jwtUtil, ObjectMapper objectMapper) {
        super(Config.class);
        this.validator = validator;
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }

    public static class Config {
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            if (validator.isSecured(request)) {

                if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                    log.warn("Missing Authorization header for secured endpoint: {}",
                            request.getPath());
                    return onError(exchange, "Authorization header is required",
                            HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED);
                }

                String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    log.warn("Invalid Authorization header format: {}", authHeader);
                    return onError(exchange, "Invalid Authorization header format. Expected: Bearer <token>",
                            HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED);
                }

                String token = authHeader.substring(7);

                try {
                    if (!jwtUtil.validateToken(token)) {
                        log.warn("Invalid or expired token for path: {}", request.getPath());
                        return onError(exchange, "Token is invalid or expired",
                                HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED);
                    }

                    String userId = jwtUtil.getUserIdFromToken(token);

                    if (userId == null || userId.isEmpty()) {
                        log.error("Token validation passed but userId is null");
                        return onError(exchange, "Invalid token claims",
                                HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED);
                    }

                    request = exchange.getRequest()
                            .mutate()
                            .header("X-User-Id", userId)
                            .header("X-Auth-Token", token)
                            .build();

                    log.debug("Authentication successful. User: {}, Path: {}",
                            userId, request.getPath());

                } catch (ExpiredJwtException e) {
                    log.warn("Expired JWT token: {}", e.getMessage());
                    return onError(exchange, "Token has expired",
                            HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED);
                } catch (Exception e) {
                    log.error("JWT authentication failed for path: {}", request.getPath(), e);
                    return onError(exchange, "Authentication failed: " + e.getMessage(),
                            HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED);
                }
            }

            return chain.filter(exchange.mutate().request(request).build());
        };
    }

    /**
     * Отправка ошибки клиенту
     */
    private Mono<Void> onError(ServerWebExchange exchange,
                               String message,
                               HttpStatus status,
                               ErrorCode errorCode) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(message)
                .build();

        ApiResponse<Void> apiResponse = ApiResponse.error(errorResponse);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(apiResponse);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("Error writing authentication error response", e);
            return response.setComplete();
        }
    }


}
