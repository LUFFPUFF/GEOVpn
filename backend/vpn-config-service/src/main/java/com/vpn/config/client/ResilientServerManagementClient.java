package com.vpn.config.client;

import com.vpn.common.dto.ApiResponse;
import com.vpn.common.dto.ServerDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Wrapper для ServerManagementClient с Circuit Breaker
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResilientServerManagementClient {

    private final ServerManagementClient serverManagementClient;

    /**
     * Получить активные серверы с Circuit Breaker и Retry
     */
    @Cacheable(value = "active-servers", unless = "#result.isEmpty()")
    @CircuitBreaker(name = "serverManagement", fallbackMethod = "getActiveServersFallback")
    @Retry(name = "serverManagement")
    public List<ServerDto> getActiveServers() {
        log.debug("Calling Server Management Service for active servers");

        ApiResponse<List<ServerDto>> response = serverManagementClient.getActiveServers();

        if (response != null && response.getData() != null) {
            return response.getData();
        }

        return Collections.emptyList();
    }

    /**
     * Fallback метод если Server Management Service недоступен
     */
    private List<ServerDto> getActiveServersFallback(Exception e) {
        log.error("Server Management Service is unavailable, using fallback. Error: {}",
                e.getMessage());

        // todo В production можно вернуть кешированные серверы
        // Сейчас возвращается пустой список
        return Collections.emptyList();
    }

    /**
     * Получить сервер по ID с Circuit Breaker
     */
    @Cacheable(value = "server", key = "#id")
    @CircuitBreaker(name = "serverManagement", fallbackMethod = "getServerByIdFallback")
    @Retry(name = "serverManagement")
    public ServerDto getServerById(Integer id) {
        ApiResponse<ServerDto> response = serverManagementClient.getServerById(id);
        return (response != null) ? response.getData() : null;
    }

    /**
     * Fallback для getServerById
     */
    private ServerDto getServerByIdFallback(Integer id, Exception e) {
        log.error("Failed to fetch server {}: {}", id, e.getMessage());
        return null;
    }
}
