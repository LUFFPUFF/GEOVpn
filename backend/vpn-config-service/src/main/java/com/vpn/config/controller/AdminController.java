package com.vpn.config.controller;

import com.vpn.common.dto.ApiResponse;
import com.vpn.common.dto.response.VpnConfigResponse;
import com.vpn.common.security.UserRole;
import com.vpn.common.security.annotations.RequireAnyRole;
import com.vpn.config.client.UserServiceClient;
import com.vpn.config.service.VpnAbuseMonitorJob;
import com.vpn.config.service.VpnSyncSchedulerService;
import com.vpn.config.service.interf.VpnConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;


@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final VpnConfigService vpnConfigService;
    private final VpnSyncSchedulerService syncSchedulerService;
    private final VpnAbuseMonitorJob abuseMonitorJob;

    @GetMapping("/devices/{deviceId}/config")
    @RequireAnyRole({UserRole.ADMIN})
    public ResponseEntity<ApiResponse<VpnConfigResponse>> getDeviceConfig(@PathVariable Long deviceId) {
        VpnConfigResponse response = vpnConfigService.getConfigByDeviceId(deviceId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/sync")
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
    public ResponseEntity<String> unbanUser(@PathVariable Long userId) {
        log.info("Admin command: Unban requested for userId={}", userId);

        long startTime = System.currentTimeMillis();
        syncSchedulerService.unbanUser(userId);
        long duration = System.currentTimeMillis() - startTime;

        return ResponseEntity.ok("User " + userId + " has been successfully unbanned in " + duration + " ms");
    }

    @GetMapping("/sync/user/{userId}/domains")
    public ResponseEntity<Map<String, Set<String>>> getUserDomains(@PathVariable Long userId) {
        log.info("Admin command: On-demand domains requested for userId={}", userId);

        Map<String, Set<String>> domainsMap = abuseMonitorJob.getUserDomainsOnDemand(userId);

        return ResponseEntity.ok(domainsMap);
    }

    @PostMapping("/sync/ban/{userId}")
    public ResponseEntity<String> banUser(
            @PathVariable Long userId,
            @org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "MANUAL_ADMIN_BAN") String reason
    ) {
        log.info("Admin command: Manual ban requested for userId={}, reason='{}'", userId, reason);

        long startTime = System.currentTimeMillis();
        syncSchedulerService.banUserManually(userId, reason);
        long duration = System.currentTimeMillis() - startTime;

        return ResponseEntity.ok("User " + userId + " has been successfully banned in " + duration + " ms");
    }

}
