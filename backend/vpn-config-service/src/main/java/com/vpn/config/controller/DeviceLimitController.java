package com.vpn.config.controller;

import com.vpn.common.dto.ApiResponse;
import com.vpn.common.security.annotations.RequireAdmin;
import com.vpn.common.security.annotations.RequireUser;
import com.vpn.config.service.DeviceLimitService;
import com.vpn.config.service.DeviceLimitService.DeviceLimitStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class DeviceLimitController {

    private final DeviceLimitService deviceLimitService;

    @GetMapping("/api/v1/admin/device-limits/{userId}")
    @RequireAdmin
    public ResponseEntity<ApiResponse<DeviceLimitStatus>> getUserLimit(
            @PathVariable Long userId) {

        log.info("Admin: getting device limit for user: {}", userId);
        return ResponseEntity.ok(
                ApiResponse.success(deviceLimitService.getStatus(userId)));
    }

    @PostMapping("/api/v1/admin/device-limits/{userId}")
    @RequireAdmin
    public ResponseEntity<ApiResponse<DeviceLimitStatus>> setLimit(
            @PathVariable Long userId,
            @Valid @RequestBody SetLimitRequest request) {

        log.info("Admin: setting limit for user: {}, plan: {}, max: {}",
                userId, request.getPlanName(), request.getMaxDevices());

        deviceLimitService.setDeviceLimit(
                userId,
                request.getMaxDevices(),
                request.getPlanName(),
                request.getExpiresAt()
        );

        return ResponseEntity.ok(
                ApiResponse.success(deviceLimitService.getStatus(userId)));
    }

    @PutMapping("/api/v1/admin/device-limits/{userId}/plan")
    @RequireAdmin
    public ResponseEntity<ApiResponse<DeviceLimitStatus>> changePlan(
            @PathVariable Long userId,
            @Valid @RequestBody ChangePlanRequest request) {

        log.info("Admin: changing plan for user: {} to {}",
                userId, request.getPlanName());

        Plan plan = Plan.fromName(request.getPlanName());
        deviceLimitService.setDeviceLimit(
                userId,
                plan.getMaxDevices(),
                plan.name(),
                request.getExpiresAt()
        );

        return ResponseEntity.ok(
                ApiResponse.success(deviceLimitService.getStatus(userId)));
    }

    @DeleteMapping("/api/v1/admin/device-limits/{userId}")
    @RequireAdmin
    public ResponseEntity<ApiResponse<Void>> resetLimit(
            @PathVariable Long userId) {

        log.info("Admin: resetting device limit for user: {}", userId);
        deviceLimitService.setDeviceLimit(userId, 1, "BASIC", null);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/api/v1/admin/device-limits/bulk")
    @RequireAdmin
    public ResponseEntity<ApiResponse<BulkResult>> bulkSetLimits(
            @Valid @RequestBody BulkSetLimitRequest request) {

        log.info("Admin: bulk limits for {} users, plan: {}",
                request.getUserIds().size(), request.getPlanName());

        Plan plan = Plan.fromName(request.getPlanName());
        int success = 0;
        int failed = 0;

        for (Long userId : request.getUserIds()) {
            try {
                deviceLimitService.setDeviceLimit(
                        userId,
                        plan.getMaxDevices(),
                        plan.name(),
                        request.getExpiresAt()
                );
                success++;
            } catch (Exception e) {
                log.warn("Failed to set limit for user {}: {}",
                        userId, e.getMessage());
                failed++;
            }
        }

        return ResponseEntity.ok(ApiResponse.success(
                BulkResult.builder()
                        .total(request.getUserIds().size())
                        .success(success)
                        .failed(failed)
                        .build()));
    }

    @GetMapping("/api/v1/devices/limit")
    @RequireUser
    public ResponseEntity<ApiResponse<DeviceLimitStatus>> getMyLimit(
            @RequestHeader("X-User-Id") Long telegramId) {

        return ResponseEntity.ok(
                ApiResponse.success(deviceLimitService.getStatus(telegramId)));
    }


    @Data
    public static class SetLimitRequest {
        @NotNull
        @Min(1) @Max(100)
        private Integer maxDevices;

        @NotNull
        private String planName;

        private LocalDateTime expiresAt;
    }

    @Data
    public static class ChangePlanRequest {
        @NotNull
        private String planName;

        private LocalDateTime expiresAt;
    }

    @Data
    public static class BulkSetLimitRequest {
        @NotNull
        private List<Long> userIds;

        @NotNull
        private String planName;

        private LocalDateTime expiresAt;
    }

    @Data
    @Builder
    public static class BulkResult {
        private int total;
        private int success;
        private int failed;
    }

    @Getter
    public enum Plan {
        BASIC(1),
        STANDARD(3),
        FAMILY(5),
        BUSINESS(10),
        UNLIMITED(100);

        private final int maxDevices;

        Plan(int maxDevices) { this.maxDevices = maxDevices; }

        public static Plan fromName(String name) {
            try {
                return Plan.valueOf(name.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Unknown plan: " + name +
                                ". Available: BASIC, STANDARD, FAMILY, BUSINESS, UNLIMITED");
            }
        }
    }
}
