package com.vpn.gateway.security;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Валидатор для определения защищенных endpoints
 * Если endpoint в списке openEndpoints - аутентификация не требуется
 */
@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "service.security")
public class RouteValidator {

    private String openEndpoints;
    private List<String> openEndpointsList;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @PostConstruct
    public void init() {
        if (openEndpoints != null) {
            openEndpointsList = Arrays.stream(openEndpoints.split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());
            log.info("Initialized RouteValidator with {} open endpoints", openEndpointsList.size());
            openEndpointsList.forEach(endpoint -> log.debug("  - {}", endpoint));
        }
    }

    public boolean isSecured(ServerHttpRequest request) {
        String path = request.getURI().getPath();

        boolean isOpen = openEndpointsList.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));

        if (isOpen) {
            log.debug("ALLOWED: Open endpoint accessed: {}", path);
        } else {
            log.debug("SECURED: Token required for: {}", path);
        }

        return !isOpen;
    }
}
