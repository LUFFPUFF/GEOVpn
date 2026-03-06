package com.vpn.gateway.security;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Валидатор для определения защищенных endpoints
 * Если endpoint в списке openEndpoints - аутентификация не требуется
 */
@Slf4j
@Component
public class RouteValidator {

    @Value("${service.security.open-endpoints}")
    private List<String> openEndpoints;

    @PostConstruct
    public void init() {
        log.info("Initialized RouteValidator with {} open endpoints", openEndpoints.size());
        openEndpoints.forEach(endpoint -> log.debug("  - {}", endpoint));
    }

    /**
     * Проверяет требуется ли аутентификация для данного запроса
     * @return true если endpoint защищен и требует аутентификации
     */
    public boolean isSecured(ServerHttpRequest request) {
        String path = request.getURI().getPath();

        boolean isOpen = openEndpoints.stream()
                .anyMatch(path::startsWith);

        if (isOpen) {
            log.debug("Open endpoint accessed: {}", path);
        } else {
            log.debug("Secured endpoint accessed: {}", path);
        }

        return !isOpen;
    }

    /**
     * Проверить конкретный path
     */
    public boolean isEndpointOpen(String path) {
        return openEndpoints.stream().anyMatch(path::startsWith);
    }
}
