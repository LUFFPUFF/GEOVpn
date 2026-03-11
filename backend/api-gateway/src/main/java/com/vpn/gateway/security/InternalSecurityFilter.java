package com.vpn.gateway.security;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global Filter для добавления internal secret в запросы к микросервисам
 * Гарантирует что микросервисы могут принимать запросы только от Gateway
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InternalSecurityFilter implements GlobalFilter, Ordered {

    @Value("${service.security.internal-secret}")
    private String internalSecret;


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        if (request.getPath().value().startsWith("/actuator")) {
            return chain.filter(exchange);
        }

        ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-Internal-Secret", internalSecret)
                .build();

        log.debug("Added internal security header to request: {}",
                request.getPath());

        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
