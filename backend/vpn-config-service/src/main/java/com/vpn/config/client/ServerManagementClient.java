package com.vpn.config.client;

import com.vpn.common.dto.ApiResponse;
import com.vpn.common.dto.ServerDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * Feign Client для Server Management Service
 */
@FeignClient(
        name = "server-management-service",
        url = "${SERVER_MGM_SERVICE_URL}",
        configuration = com.vpn.common.config.FeignClientConfiguration.class
)
public interface ServerManagementClient {

    /**
     * Получить все активные серверы
     */
    @GetMapping("/api/v1/servers/active")
    ApiResponse<List<ServerDto>> getActiveServers();

    /**
     * Получить информацию о конкретном сервере
     */
    @GetMapping("/api/v1/servers/{id}")
    ApiResponse<ServerDto> getServerById(@PathVariable("id") Integer id);

    /**
     * Получить health метрики сервера
     */
    @GetMapping("/api/v1/servers/{id}/health")
    ServerDto getServerHealth(@PathVariable("id") Integer id);
}
