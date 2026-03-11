package com.vpn.server.controller;

import com.vpn.common.dto.ApiResponse;
import com.vpn.common.dto.ServerDto;
import com.vpn.common.security.UserRole;
import com.vpn.common.security.annotations.RequireAdmin;
import com.vpn.common.security.annotations.RequireAnyRole;
import com.vpn.server.dto.CreateServerRequest;
import com.vpn.server.dto.UpdateServerRequest;
import com.vpn.server.service.ServerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/servers")
@RequiredArgsConstructor
public class ServerController {

    private final ServerService serverService;

    /**
     * Зарегистрировать новый сервер (ADMIN only)
     * POST /api/v1/servers
     */
    @PostMapping
    @RequireAdmin
    public ResponseEntity<ApiResponse<ServerDto>> createServer(
            @RequestBody @Valid CreateServerRequest request
    ) {
        ServerDto created = serverService.createServer(request);
        return ResponseEntity.ok(ApiResponse.success(created));
    }

    /**
     * Обновить сервер
     * PUT /api/v1/servers/{id}
     */
    @PutMapping("/{id}")
    @RequireAdmin
    public ResponseEntity<ApiResponse<ServerDto>> updateServer(
            @PathVariable Integer id,
            @RequestBody @Valid UpdateServerRequest request
    ) {
        ServerDto updated = serverService.updateServer(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @DeleteMapping("/{id}")
    @RequireAdmin
    public ResponseEntity<ApiResponse<Void>> deleteServer(@PathVariable Integer id) {
        serverService.deleteServer(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Получить список всех активных серверов
     * Доступно: Микросервисам (Config Service) и Админам
     */
    @GetMapping("/active")
    @RequireAnyRole({UserRole.ADMIN, UserRole.SERVICE})
    public ResponseEntity<ApiResponse<List<ServerDto>>> getActiveServers() {
        return ResponseEntity.ok(ApiResponse.success(serverService.getAllActiveServers()));
    }

    /**
     * Получить детальную инфу о сервере
     */
    @GetMapping("/{id}")
    @RequireAnyRole({UserRole.ADMIN, UserRole.SERVICE})
    public ResponseEntity<ApiResponse<ServerDto>> getServerById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(serverService.getServerById(id)));
    }
}
