package com.vpn.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Configuration
public class RateLimiterConfig {

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");

            if (userId != null && !userId.isEmpty()) {
                return Mono.just("user:" + userId);
            }

            String ipAddress = exchange.getRequest().getRemoteAddress() != null
                    ? Objects.requireNonNull(exchange.getRequest().getRemoteAddress()).getAddress().getHostAddress()
                    : "unknown-ip";

            return Mono.just("ip:" + ipAddress);
        };
    }
}
