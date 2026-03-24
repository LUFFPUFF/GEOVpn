package com.vpn.config.controller;

import com.vpn.common.constant.ErrorCode;
import com.vpn.common.dto.ApiResponse;
import com.vpn.common.dto.ErrorResponse;
import com.vpn.common.security.UserRole;
import com.vpn.common.security.annotations.RequireAnyRole;
import com.vpn.common.security.annotations.RequireUser;
import com.vpn.common.util.StringUtils;
import com.vpn.common.dto.request.ConfigCreateRequest;
import com.vpn.common.dto.request.ConfigRegenerateRequest;
import com.vpn.common.dto.response.VpnConfigResponse;
import com.vpn.config.service.interf.VpnConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/configs")
@RequiredArgsConstructor
public class VpnConfigController {

    private final VpnConfigService vpnConfigService;

    /**
     * Создать новую VPN подписку (набор серверов) для устройства.
     */
    @PostMapping()
    @RequireAnyRole({UserRole.USER, UserRole.SERVICE})
    public ResponseEntity<ApiResponse<VpnConfigResponse>> createConfig(
            @RequestHeader("X-User-Id") Long telegramId,
            @Valid @RequestBody ConfigCreateRequest request) {

        request.setUserId(telegramId);
        VpnConfigResponse response = vpnConfigService.createConfig(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * Эндпоинт для внешних приложений (V2Box, Happ).
     * Отдает список серверов в формате Base64.
     * Доступен без заголовка X-User-Id по UUID ссылки.
     */
    @GetMapping(value = "/subscription/{uuid}", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getSubscription(@PathVariable("uuid") UUID vlessUuid) {
        log.info("Subscription requested for UUID: {}", vlessUuid);
        String base64Content = vpnConfigService.getSubscription(vlessUuid);
        return ResponseEntity.ok(base64Content);
    }

    /**
     * Получить все активные подписки пользователя.
     */
    @GetMapping("/configs")
    @RequireUser
    public ResponseEntity<ApiResponse<List<VpnConfigResponse>>> getMyConfigs(
            @RequestHeader("X-User-Id") Long telegramId) {

        List<VpnConfigResponse> configs = vpnConfigService.getActiveConfigs(telegramId);
        return ResponseEntity.ok(ApiResponse.success(configs));
    }

    /**
     * Детальная информация о подписке по ID устройства.
     */
    @GetMapping("/configs/{deviceId}")
    @RequireUser
    public ResponseEntity<ApiResponse<VpnConfigResponse>> getConfigByDeviceId(
            @RequestHeader("X-User-Id") Long telegramId,
            @PathVariable("deviceId") Long deviceId) {

        if (!vpnConfigService.isConfigOwnedByUser(deviceId, telegramId)) {
            return buildConfigNotFoundResponse();
        }

        VpnConfigResponse response = vpnConfigService.getConfigByDeviceId(deviceId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Перевыпуск подписки (смена UUID).
     */
    @PutMapping("/configs/{deviceId}/regenerate")
    @RequireUser
    public ResponseEntity<ApiResponse<VpnConfigResponse>> regenerateConfig(
            @RequestHeader("X-User-Id") Long telegramId,
            @PathVariable("deviceId") Long deviceId,
            @Valid @RequestBody ConfigRegenerateRequest request) {

        if (!vpnConfigService.isConfigOwnedByUser(deviceId, telegramId)) {
            return buildConfigNotFoundResponse();
        }

        VpnConfigResponse response = vpnConfigService.regenerateConfig(deviceId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Удаление подписки (отзыв доступа на серверах).
     */
    @DeleteMapping("/configs/{deviceId}")
    @RequireUser
    public ResponseEntity<ApiResponse<Void>> revokeConfig(
            @RequestHeader("X-User-Id") Long telegramId,
            @PathVariable("deviceId") Long deviceId) {

        vpnConfigService.revokeConfig(deviceId, telegramId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private ResponseEntity<ApiResponse<VpnConfigResponse>> buildConfigNotFoundResponse() {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(ErrorCode.CONFIG_GENERATION_FAILED.getCode())
                .message("Configuration not found")
                .traceId(StringUtils.generateUuid())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(errorResponse));
    }
}
