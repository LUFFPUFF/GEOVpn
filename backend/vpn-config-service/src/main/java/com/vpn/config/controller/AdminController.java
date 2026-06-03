package com.vpn.config.controller;

import com.vpn.common.dto.ApiResponse;
import com.vpn.common.security.UserRole;
import com.vpn.common.security.annotations.RequireAnyRole;
import com.vpn.config.dto.admin.AdminConfigDetailResponse;
import com.vpn.config.dto.admin.AdminConfigUpdateRequest;
import com.vpn.config.service.GlobalMaintenanceService;
import com.vpn.config.service.VpnAbuseMonitorJob;
import com.vpn.config.service.VpnSyncSchedulerService;
import com.vpn.config.service.interf.VpnConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;


@Slf4j
@RestController
@RequestMapping("/api/v1/configs/admin")
@RequiredArgsConstructor
public class AdminController {

    private final VpnConfigService              vpnConfigService;
    private final VpnSyncSchedulerService       syncSchedulerService;
    private final VpnAbuseMonitorJob            abuseMonitorJob;
    private final GlobalMaintenanceService      globalMaintenanceService;

    @GetMapping("/devices/{deviceId}/config/details")
    @RequireAnyRole({UserRole.ADMIN})
    public ResponseEntity<ApiResponse<AdminConfigDetailResponse>> getDeviceConfigDetails(
            @PathVariable Long deviceId) {

        AdminConfigDetailResponse response = vpnConfigService.getAdminConfigDetails(deviceId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/devices/{deviceId}/config")
    @RequireAnyRole({UserRole.ADMIN})
    public ResponseEntity<ApiResponse<AdminConfigDetailResponse>> updateDeviceConfig(
            @PathVariable Long deviceId,
            @RequestBody AdminConfigUpdateRequest request) {

        AdminConfigDetailResponse response = vpnConfigService.updateConfigAndServers(deviceId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Запускает полный цикл обслуживания в фоне и сразу возвращает 202 Accepted.
     *
     * <p>Прогресс доступен через SSE-стрим {@code /maintenance/stream}.
     */
    @PostMapping("/maintenance/global-reset")
    @RequireAnyRole({UserRole.ADMIN})
    public ResponseEntity<String> runGlobalMaintenance() {
        log.warn("GLOBAL MAINTENANCE triggered by admin via REST API.");

        CompletableFuture.runAsync(() -> {
            try {
                GlobalMaintenanceService.MaintenanceReport report =
                        globalMaintenanceService.runFullMaintenance();
                log.info("GLOBAL MAINTENANCE finished: {}", report);
            } catch (Exception e) {
                log.error("GLOBAL MAINTENANCE failed with exception", e);
            }
        });

        return ResponseEntity.accepted().body(
                "Global maintenance started in background.\n" +
                        "Connect to /api/v1/configs/admin/maintenance/stream (SSE) to monitor progress.\n" +
                        "Steps: 1) Token update → 2) XUI panels clear → 3) DB + Redis wipe → 4) Batch regeneration & XUI sync."
        );
    }

    @GetMapping(value = "/maintenance/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @com.vpn.common.security.annotations.Public
    public SseEmitter streamMaintenanceLogs() {
        log.info("Admin SSE client connected to maintenance stream. Active emitters: ...");
        return globalMaintenanceService.registerEmitter();
    }


    @PostMapping("/sync")
    @RequireAnyRole({UserRole.ADMIN})
    public ResponseEntity<String> forceSync() {
        log.info("Manual sync triggered via REST API (Asynchronous)");

        CompletableFuture.runAsync(() -> {
            try {
                syncSchedulerService.synchronizeAllActiveConfigs();
            } catch (Exception e) {
                log.error("Background sync failed", e);
            }
        });

        return ResponseEntity.accepted()
                .body("Synchronization started in background. Please monitor the server logs for progress.");
    }

    @PostMapping("/sync/unban/{userId}")
    @RequireAnyRole({UserRole.ADMIN})
    public ResponseEntity<String> unbanUser(@PathVariable Long userId) {
        log.info("Admin command: Unban requested for userId={}", userId);

        long startTime = System.currentTimeMillis();
        syncSchedulerService.unbanUser(userId);
        long duration = System.currentTimeMillis() - startTime;

        return ResponseEntity.ok("User " + userId + " has been successfully unbanned in " + duration + " ms");
    }

    @GetMapping("/sync/user/{userId}/domains")
    @RequireAnyRole({UserRole.ADMIN})
    public ResponseEntity<Map<String, Set<String>>> getUserDomains(@PathVariable Long userId) {
        log.info("Admin command: On-demand domains requested for userId={}", userId);

        Map<String, Set<String>> domainsMap = abuseMonitorJob.getUserDomainsOnDemand(userId);
        return ResponseEntity.ok(domainsMap);
    }

    @PostMapping("/sync/ban/{userId}")
    @RequireAnyRole({UserRole.ADMIN})
    public ResponseEntity<String> banUser(
            @PathVariable Long userId,
            @RequestParam(required = false, defaultValue = "MANUAL_ADMIN_BAN") String reason) {

        log.info("Admin command: Manual ban requested for userId={}, reason='{}'", userId, reason);

        long startTime = System.currentTimeMillis();
        syncSchedulerService.banUserManually(userId, reason);
        long duration = System.currentTimeMillis() - startTime;

        return ResponseEntity.ok("User " + userId + " has been successfully banned in " + duration + " ms");
    }
}