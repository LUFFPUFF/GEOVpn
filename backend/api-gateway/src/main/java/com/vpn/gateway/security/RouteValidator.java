package com.vpn.gateway.security;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Валидатор для определения защищенных endpoints
 * Если endpoint в списке openEndpoints - аутентификация не требуется
 */
@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "service.security")
public class RouteValidator {

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
