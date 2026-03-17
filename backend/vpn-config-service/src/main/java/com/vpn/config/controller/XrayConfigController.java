package com.vpn.config.controller;

import com.vpn.common.dto.ApiResponse;
import com.vpn.common.security.annotations.RequireUser;
import com.vpn.common.security.context.SecurityContextHolder;
import com.vpn.config.service.rules.XrayConfigGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequestMapping("/api/v1/configs")
@RequiredArgsConstructor
public class XrayConfigController {

    private final XrayConfigGenerator xrayConfigGenerator;

    /**
     * Получить полный Xray JSON конфиг для устройства
     *
     * GET /api/v1/configs/{deviceId}/xray.json
     *
     * Возвращает JSON файл для импорта в клиент
     */
    @GetMapping("/{deviceId}/xray.json")
    @RequireUser
    public ResponseEntity<String> getXrayJsonConfig(@PathVariable Long deviceId) {
        Long userId = SecurityContextHolder.getUserId();

        log.info("Запрос полного Xray конфига: userId={}, deviceId={}", userId, deviceId);

        // TODO: Проверить что deviceId принадлежит userId

        String jsonConfig = xrayConfigGenerator.generateFullConfig(deviceId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"xray-config-" + deviceId + ".json\"")
                .body(jsonConfig);
    }

    /**
     * Получить URL для скачивания конфига
     *
     * GET /api/v1/configs/{deviceId}/download-url
     *
     * Возвращает URL который можно использовать для импорта
     */
    @GetMapping("/{deviceId}/download-url")
    @RequireUser
    public ResponseEntity<ApiResponse<String>> getConfigDownloadUrl(
            @PathVariable Long deviceId
    ) {
        Long userId = SecurityContextHolder.getUserId();

        log.info("🔗 Запрос URL для скачивания конфига: userId={}, deviceId={}",
                userId, deviceId);

        String downloadUrl = String.format(
                "https://api.geovpn.com/api/v1/configs/%d/xray.json",
                deviceId
        );

        return ResponseEntity.ok(ApiResponse.success(downloadUrl));
    }
}
