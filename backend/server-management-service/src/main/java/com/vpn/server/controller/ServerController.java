package com.vpn.server.controller;

import com.vpn.common.dto.ApiResponse;
import com.vpn.common.dto.ServerDto;
import com.vpn.common.security.UserRole;
import com.vpn.common.security.annotations.RequireAdmin;
import com.vpn.common.security.annotations.RequireAnyRole;
import com.vpn.server.dto.CreateServerRequest;
import com.vpn.server.dto.SystemHealthDto;
import com.vpn.server.dto.UpdateServerRequest;
import com.vpn.server.dto.UserStatDto;
import com.vpn.server.grpc.XrayGrpcClient;
import com.vpn.server.service.ServerService;
import com.vpn.server.service.SystemHealthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/servers")
@RequiredArgsConstructor
public class ServerController {

    private final ServerService serverService;
    private final XrayGrpcClient xrayGrpcClient;
    private final SystemHealthService systemHealthService;

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

    @GetMapping
    @RequireAdmin
    public ResponseEntity<ApiResponse<List<ServerDto>>> getAllServers() {
        return ResponseEntity.ok(ApiResponse.success(serverService.getAllServers()));
    }

    @GetMapping("/{id}/users")
    @RequireAdmin
    public ResponseEntity<ApiResponse<List<UserStatDto>>> getServerUsers(@PathVariable Integer id) {
        ServerDto server = serverService.getServerById(id);

        List<UserStatDto> userStats = List.of();

        for (int i = 0; i < 3; i++) {
            List<com.vpn.server.grpc.generated.Stat> allStats =
                    xrayGrpcClient.getAllStatistics(server.getIpAddress(), 62789);

            userStats = allStats.stream()
                    .filter(s -> {
                        s.getName();
                        return s.getName().contains("user>>>");
                    })
                    .map(s -> UserStatDto.builder().name(s.getName()).value(s.getValue()).build())
                    .toList();

            if (!userStats.isEmpty()) break;
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        }

        return ResponseEntity.ok(ApiResponse.success(userStats));
    }

    @PostMapping("/{id}/users/{email}/kick")
    @RequireAdmin
    public ResponseEntity<ApiResponse<Boolean>> kickUser(
            @PathVariable Integer id,
            @PathVariable String email) {

        ServerDto server = serverService.getServerById(id);
        boolean success = xrayGrpcClient.removeUser(server.getIpAddress(), 62789, "vless-reality-lte", email);
        return ResponseEntity.ok(ApiResponse.success(success));
    }

    @GetMapping("/infrastructure/health")
    @RequireAdmin
    public ResponseEntity<ApiResponse<SystemHealthDto>> getInfrastructureHealth() {
        return ResponseEntity.ok(ApiResponse.success(systemHealthService.getClusterHealth()));
    }
}
