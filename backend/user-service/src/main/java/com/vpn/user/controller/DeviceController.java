package com.vpn.user.controller;

import com.vpn.common.constant.ErrorCode;
import com.vpn.common.dto.ApiResponse;
import com.vpn.common.dto.ErrorResponse;
import com.vpn.common.dto.request.DeviceCreateRequest;
import com.vpn.common.dto.response.DeviceResponse;
import com.vpn.common.security.annotations.RequireUser;
import com.vpn.user.service.interf.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    /**
     * Зарегистрировать новое устройство для текущего пользователя
     */
    @PostMapping
    @RequireUser
    public ResponseEntity<ApiResponse<DeviceResponse>> registerDevice(
            @RequestHeader("X-User-Id") Long telegramId,
            @Valid @RequestBody DeviceCreateRequest request) {

        request.setUserId(telegramId);

        DeviceResponse response = deviceService.createDevice(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @PostMapping("/sync")
    @RequireUser
    public ResponseEntity<ApiResponse<DeviceResponse>> syncDevice(
            @RequestHeader("X-User-Id") Long telegramId,
            @RequestBody Map<String, String> payload) {

        String platform = payload.get("platform");

        DeviceResponse response = deviceService.syncDeviceWithPlatform(telegramId, platform);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Получить список всех активных устройств текущего пользователя
     */
    @GetMapping
    @RequireUser
    public ResponseEntity<ApiResponse<List<DeviceResponse>>> getMyDevices(
            @RequestHeader("X-User-Id") Long telegramId) {

        List<DeviceResponse> devices = deviceService.getUserActiveDevices(telegramId);
        return ResponseEntity.ok(ApiResponse.success(devices));
    }

    /**
     * Получить информацию о конкретном устройстве по UUID
     * ВНИМАНИЕ: Нужно проверить, что девайс принадлежит этому пользователю!
     */
    @GetMapping("/{uuid}")
    @RequireUser
    public ResponseEntity<ApiResponse<DeviceResponse>> getDevice(
            @RequestHeader("X-User-Id") Long telegramId,
            @PathVariable("uuid") UUID uuid) {

        if (!deviceService.isDeviceOwnedByUser(uuid, telegramId)) {

            ErrorResponse errorResponse = ErrorResponse.builder()
                    .code(ErrorCode.DEVICE_NOT_FOUND.getCode())
                    .message(ErrorCode.DEVICE_NOT_FOUND.getDefaultMessage())
                    .traceId("") //todo указать верный traceId
                    .build();

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(errorResponse));
        }

        DeviceResponse response = deviceService.getDeviceByUuid(uuid);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Деактивировать устройство (Soft Delete)
     */
    @DeleteMapping("/{uuid}")
    @RequireUser
    public ResponseEntity<ApiResponse<Void>> deactivateDevice(
            @RequestHeader("X-User-Id") Long telegramId,
            @PathVariable("uuid") UUID uuid) {

        deviceService.deactivateDevice(uuid, telegramId);

        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
