package com.vpn.config.controller;

import com.vpn.common.dto.ApiResponse;
import com.vpn.common.security.annotations.RequireUser;
import com.vpn.common.security.context.SecurityContextHolder;
import com.vpn.config.generator.shadowsocks.ShadowsocksConfigGenerator;
import com.vpn.config.service.interf.VpnConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Контроллер для Shadowsocks конфигураций
 *
 * Опциональный - используется если нужно получить ТОЛЬКО SS конфиг
 * По умолчанию SS включен в основной VpnConfigResponse
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/configs")
@RequiredArgsConstructor
public class ShadowsocksConfigController {

    private final VpnConfigService vpnConfigService;
    private final ShadowsocksConfigGenerator shadowsocksConfigGenerator;

    /**
     * Получить Shadowsocks JSON конфиг для устройства
     *
     * GET /api/v1/configs/{deviceId}/shadowsocks.json
     *
     * Возвращает JSON для импорта в SS клиент
     */
    @GetMapping("/{deviceId}/shadowsocks.json")
    @RequireUser
    public ResponseEntity<Map<String, Object>> getShadowsocksJsonConfig(
            @PathVariable Long deviceId
    ) {
        Long userId = SecurityContextHolder.getUserId();

        log.info("Запрос Shadowsocks конфига: userId={}, deviceId={}", userId, deviceId);

        // TODO: Проверить что deviceId принадлежит userId

        Map<String, Object> ssConfig = vpnConfigService.getShadowsocksConfig(deviceId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"shadowsocks-config-" + deviceId + ".json\"")
                .body(ssConfig);
    }

    /**
     * Получить только Shadowsocks ссылку
     *
     * GET /api/v1/configs/{deviceId}/shadowsocks-link
     */
    @GetMapping("/{deviceId}/shadowsocks-link")
    @RequireUser
    public ResponseEntity<ApiResponse<String>> getShadowsocksLink(
            @PathVariable Long deviceId
    ) {
        Long userId = SecurityContextHolder.getUserId();

        log.info("Запрос SS ссылки: userId={}, deviceId={}", userId, deviceId);

        String ssLink = vpnConfigService.getShadowsocksLink(deviceId);

        return ResponseEntity.ok(ApiResponse.success(ssLink));
    }

    /**
     * Проверить доступность Shadowsocks
     *
     * GET /api/v1/configs/shadowsocks/status
     */
    @GetMapping("/shadowsocks/status")
    @RequireUser
    public ResponseEntity<ApiResponse<Map<String, Object>>> getShadowsocksStatus() {
        log.debug("Проверка статуса Shadowsocks");

        Map<String, Object> status = shadowsocksConfigGenerator.getShadowsocksInfo();

        return ResponseEntity.ok(ApiResponse.success(status));
    }
}
