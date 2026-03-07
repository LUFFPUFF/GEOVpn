package com.vpn.server.controller;

import com.vpn.common.dto.ApiResponse;
import com.vpn.common.dto.ServerDto;
import com.vpn.server.service.ServerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/servers")
@RequiredArgsConstructor
public class ServerController {

    private final ServerService serverService;

    /**
     * Получить список всех активных серверов
     * Доступно: Микросервисам (Config Service) и Админам
     */
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('SERVICE', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<ServerDto>>> getActiveServers() {
        return ResponseEntity.ok(ApiResponse.success(serverService.getAllActiveServers()));
    }

    /**
     * Получить детальную инфу о сервере
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SERVICE', 'ADMIN')")
    public ResponseEntity<ApiResponse<ServerDto>> getServerById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(serverService.getServerById(id)));
    }
}
