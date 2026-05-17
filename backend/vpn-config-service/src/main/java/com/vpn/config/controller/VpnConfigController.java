package com.vpn.config.controller;

import com.vpn.common.constant.ErrorCode;
import com.vpn.common.dto.ApiResponse;
import com.vpn.common.dto.ErrorResponse;
import com.vpn.common.security.UserRole;
import com.vpn.common.security.annotations.Public;
import com.vpn.common.security.annotations.RequireAnyRole;
import com.vpn.common.security.annotations.RequireUser;
import com.vpn.common.security.context.SecurityContextHolder;
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
    @PostMapping
    @RequireAnyRole({UserRole.USER, UserRole.SERVICE})
    public ResponseEntity<ApiResponse<VpnConfigResponse>> createConfig(
            @Valid @RequestBody ConfigCreateRequest request) {

        Long telegramId = SecurityContextHolder.getUserId();
        request.setUserTelegramId(telegramId);
        request.setUserId(telegramId);

        VpnConfigResponse response = vpnConfigService.createConfig(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
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
    public ResponseEntity<ApiResponse<List<VpnConfigResponse>>> getMyConfigs() {
        Long telegramId = SecurityContextHolder.getUserId();
        List<VpnConfigResponse> configs = vpnConfigService.getActiveConfigs(telegramId);
        return ResponseEntity.ok(ApiResponse.success(configs));
    }

    /**
     * Детальная информация о подписке по ID устройства.
     */
    @GetMapping("/configs/{deviceId}")
    @RequireUser
    public ResponseEntity<ApiResponse<VpnConfigResponse>> getConfigByDeviceId(@PathVariable Long deviceId) {
        Long telegramId = SecurityContextHolder.getUserId();
        if (!vpnConfigService.isConfigOwnedByUser(deviceId, telegramId)) {
            return buildConfigNotFoundResponse();
        }
        return ResponseEntity.ok(ApiResponse.success(vpnConfigService.getConfigByDeviceId(deviceId)));
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


    /**
     * Возвращает зашифрованную ссылку вида happ://crypt5/... для копирования.
     * При зашифрованной ссылке пользователь не видит адрес подписки и конфиги серверов.
     * Если Happ Crypto API недоступен — возвращает обычный URL подписки как fallback.
     */
    @GetMapping(value = "/encrypted-sub/{uuid}", produces = MediaType.TEXT_PLAIN_VALUE)
    @Public
    public ResponseEntity<String> getEncryptedSubLink(@PathVariable("uuid") UUID vlessUuid) {
        String baseUrl = "https://geovp.ru";
        String subscriptionUrl = baseUrl + "/api/v1/configs/subscription/" + vlessUuid;
        try {
            String encryptedLink = encryptHappSubscriptionUrl(subscriptionUrl);
            log.info("Returning encrypted sub link for uuid={}", vlessUuid);
            return ResponseEntity.ok(encryptedLink);
        } catch (Exception e) {
            log.warn("Crypto API failed, returning plain URL for copy: {}", e.getMessage());
            return ResponseEntity.ok(subscriptionUrl);
        }
    }

    @GetMapping(value = "/import-happ/{uuid}", produces = MediaType.TEXT_HTML_VALUE)
    @Public
    public ResponseEntity<String> redirectHapp(@PathVariable("uuid") UUID vlessUuid) {
        String baseUrl = "https://geovp.ru";
        String subscriptionUrl = baseUrl + "/api/v1/configs/subscription/" + vlessUuid;

        String deepLink;
        try {
            deepLink = encryptHappSubscriptionUrl(subscriptionUrl);
            log.info("Generated encrypted Happ deeplink for uuid={}", vlessUuid);
        } catch (Exception e) {
            log.warn("Happ crypto API unavailable, using plain URL fallback: {}", e.getMessage());
            String encodedUrl = java.net.URLEncoder.encode(subscriptionUrl, java.nio.charset.StandardCharsets.UTF_8);
            deepLink = "happ://add-subscription?url=" + encodedUrl;
        }

        String html = getFinalDeepLink(deepLink);

        return ResponseEntity.ok(html);
    }

    private static String getFinalDeepLink(String deepLink) {
        final String finalDeepLink = deepLink;
        String html = """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"><title>GeoVPN Import</title></head>
            <body style="background: #0a0a0f; color: white; display: flex; justify-content: center; align-items: center; height: 100vh; font-family: sans-serif; margin: 0;">
                <div style="text-align: center;">
                    <div style="font-size: 50px; margin-bottom: 20px;">🚀</div>
                    <h2>Открываем Happ Proxy...</h2>
                    <a href="%s" style="display: inline-block; margin-top: 20px; padding: 15px 30px; background: #ed8936; color: white; text-decoration: none; border-radius: 12px; font-weight: bold;">ОТКРЫТЬ HAPP</a>
                    <script>window.location.href = "%s";</script>
                </div>
            </body>
            </html>
            """.formatted(finalDeepLink, finalDeepLink);
        return html;
    }

    /**
     * Шифрует URL подписки через Happ Crypto API и возвращает
     * зашифрованный диплинк вида happ://crypt5/...
     *
     * После добавления такой подписки пользователь не может
     * редактировать, просматривать или делиться конфигурациями серверов.
     *
     * Документация: https://crypto.happ.su
     * API endpoint:  POST https://crypto.happ.su/api-v2.php
     *                Body: {"url":"<subscription_url>"}
     *                Response: happ://crypt5/<encrypted_data>
     */
    private String encryptHappSubscriptionUrl(String url) throws Exception {
        String requestBody = "{\"url\":\"" + url + "\"}";

        java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(5))
                .build();

        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("https://crypto.happ.su/api-v2.php"))
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(java.time.Duration.ofSeconds(5))
                .build();

        java.net.http.HttpResponse<String> response = client.send(
                request, java.net.http.HttpResponse.BodyHandlers.ofString());

        String encryptedLink = response.body().trim();
        if (!encryptedLink.startsWith("happ://")) {
            throw new RuntimeException("Unexpected Happ crypto API response: " + encryptedLink);
        }
        return encryptedLink;
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