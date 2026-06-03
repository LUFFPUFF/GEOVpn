package com.vpn.user.controller;

import com.vpn.common.constant.ErrorCode;
import com.vpn.common.dto.ApiResponse;
import com.vpn.common.dto.ErrorResponse;
import com.vpn.common.dto.request.DeviceCreateRequest;
import com.vpn.common.dto.response.DeviceResponse;
import com.vpn.common.security.annotations.RequireService;
import com.vpn.common.security.annotations.RequireUser;
import com.vpn.common.security.context.SecurityContextHolder;
import com.vpn.user.service.interf.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    /**
     * Зарегистрировать новое устройство для текущего пользователя.
     */
    @PostMapping
    @RequireUser
    public ResponseEntity<ApiResponse<DeviceResponse>> registerDevice(
            @Valid @RequestBody DeviceCreateRequest request) {

        request.setUserId(SecurityContextHolder.getUserId());
        DeviceResponse response = deviceService.createDevice(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Deprecated
    @PostMapping("/sync")
    @RequireUser
    public ResponseEntity<ApiResponse<DeviceResponse>> syncDevice(
            @RequestBody Map<String, String> payload) {

        Long telegramId = SecurityContextHolder.getUserId();
        String platform = payload.get("platform");
        DeviceResponse response = deviceService.syncDeviceWithPlatform(telegramId, platform);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/user/{telegramId}/active")
    @com.vpn.common.security.annotations.RequireService
    public ResponseEntity<ApiResponse<List<DeviceResponse>>> getUserActiveDevices(
            @PathVariable("telegramId") Long telegramId) {
        log.info("Internal: fetching active devices for user {}", telegramId);
        List<DeviceResponse> devices = deviceService.getUserActiveDevices(telegramId);
        return ResponseEntity.ok(ApiResponse.success(devices));
    }

    /**
     * Получить список всех активных устройств текущего пользователя.
     */
    @GetMapping
    @RequireUser
    public ResponseEntity<ApiResponse<List<DeviceResponse>>> getMyDevices() {
        Long telegramId = SecurityContextHolder.getUserId();
        List<DeviceResponse> devices = deviceService.getUserActiveDevices(telegramId);
        return ResponseEntity.ok(ApiResponse.success(devices));
    }

    /**
     * Получить информацию о конкретном устройстве по UUID.
     */
    @GetMapping("/{uuid}")
    @RequireUser
    public ResponseEntity<ApiResponse<DeviceResponse>> getDevice(
            @RequestHeader("X-User-Id") Long telegramId,
            @PathVariable("uuid") UUID uuid) {

        if (!deviceService.isDeviceOwnedByUser(uuid, telegramId)) {
            return buildDeviceNotFoundResponse();
        }

        DeviceResponse response = deviceService.getDeviceByUuid(uuid);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Деактивировать устройство — soft delete (isActive = false).
     * Устройство остаётся в БД.
     */
    @DeleteMapping("/{uuid}")
    @RequireUser
    public ResponseEntity<ApiResponse<Void>> deactivateDevice(
            @RequestHeader("X-User-Id") Long telegramId,
            @PathVariable("uuid") UUID uuid) {

        deviceService.deactivateDevice(uuid, telegramId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Физическое удаление устройства из БД — hard delete.
     * Устройство удаляется полностью. VPN конфигурация должна быть
     * отозвана заранее через DELETE /api/v1/configs/configs/{deviceId}.
     */
    @DeleteMapping("/{uuid}/permanent")
    @RequireUser
    public ResponseEntity<ApiResponse<Void>> deleteDevicePermanently(
            @RequestHeader("X-User-Id") Long telegramId,
            @PathVariable("uuid") UUID uuid) {

        deviceService.deleteDevice(uuid, telegramId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Внутренний эндпоинт для межсервисного вызова из config-service.
     * Физически удаляет устройство по его внутреннему ID при принудительном
     * соблюдении лимита устройств (например, при понижении тарифа).
     *
     * Вызывается только сервисами с ролью SERVICE, не доступен пользователям.
     */
    @DeleteMapping("/internal/{deviceId}")
    @RequireService
    public ResponseEntity<ApiResponse<Void>> deleteDeviceByIdInternal(
            @PathVariable Long deviceId,
            @RequestHeader("X-User-Id") Long userId) {

        deviceService.deleteDeviceById(deviceId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private ResponseEntity<ApiResponse<DeviceResponse>> buildDeviceNotFoundResponse() {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(ErrorCode.DEVICE_NOT_FOUND.getCode())
                .message(ErrorCode.DEVICE_NOT_FOUND.getDefaultMessage())
                .traceId("")
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(errorResponse));
    }
}