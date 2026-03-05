package com.vpn.gateway.security;

import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@Component
@ConfigurationProperties(prefix = "service.security")
@Setter
public class RouteValidator {

    private List<String> openEndpoints = new ArrayList<>();

    /**
     * Предикат, который проверяет, требует ли запрос авторизации
     */
    public Predicate<ServerHttpRequest> isSecured() {
        return request -> openEndpoints
                .stream()
                .noneMatch(uri -> request.getURI().getPath().contains(uri));
    }
}
