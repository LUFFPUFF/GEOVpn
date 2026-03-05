package com.vpn.gateway.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vpn.common.constant.ErrorCode;
import com.vpn.common.dto.ErrorResponse;
import com.vpn.common.util.StringUtils;
import com.vpn.gateway.util.JwtUtil;
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
import reactor.core.publisher.Mono;

import java.util.Objects;

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
        return (((exchange, chain) -> {

            ServerHttpRequest request = exchange.getRequest();

            if (validator.isSecured().test(request)) {
                if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                    return onError(exchange, "Missing Authorization header", HttpStatus.UNAUTHORIZED);
                }

                String authHeader = Objects.requireNonNull(request.getHeaders().get(HttpHeaders.AUTHORIZATION)).getFirst();

                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    authHeader = authHeader.substring(7);
                } else {
                    return onError(exchange, "Invalid Authorization header format", HttpStatus.UNAUTHORIZED);
                }

                try {
                    if (!jwtUtil.validateToken(authHeader)) {
                        return onError(exchange, "Token is invalid or expired", HttpStatus.UNAUTHORIZED);
                    }

                    String userId = jwtUtil.getUserIdFromToken(authHeader);

                    request = exchange.getRequest()
                            .mutate()
                            .header("X-User-Id", userId)
                            .build();
                } catch (Exception e) {
                    log.error("JWT Authentication failed", e);
                    return onError(exchange, "Authentication failed", HttpStatus.UNAUTHORIZED);
                }
            }

            return chain.filter(exchange.mutate().request(request).build());
        }));
    }

    private Mono<Void> onError(org.springframework.web.server.ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String traceId = StringUtils.generateUuid();
        log.warn("[TraceID: {}] Auth Error: {}", traceId, err);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(ErrorCode.UNAUTHORIZED.getCode())
                .message(err)
                .traceId(traceId)
                .build();

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("Error writing auth response", e);
            return response.setComplete();
        }
    }


}
