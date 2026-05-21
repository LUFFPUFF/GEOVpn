package com.vpn.config.controller;

import com.vpn.common.security.annotations.Public;
import com.vpn.common.security.annotations.RequireUser;
import com.vpn.common.security.context.SecurityContextHolder;
import com.vpn.config.domain.entity.VpnConfiguration;
import com.vpn.config.repository.VpnConfigurationRepository;
import com.vpn.config.service.DeviceSessionService;
import com.vpn.config.service.subscription.SubscriptionHeaderBuilder;
import com.vpn.common.dto.enums.ConfigStatus;
import com.vpn.config.exception.ConfigNotFoundException;
import com.vpn.config.service.subscription.SubscriptionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.servlet.view.RedirectView;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final SubscriptionHeaderBuilder headerBuilder;
    private final VpnConfigurationRepository configRepository;
    private final DeviceSessionService deviceSessionService;

    /**
     * Эндпоинт подписки для VPN-клиентов
     * GET /api/v1/subscription/{vlessUuid}
     */
    @Public
    @GetMapping("/{vlessUuid}")
    public ResponseEntity<String> getSubscription(
            @PathVariable UUID vlessUuid,
            HttpServletRequest httpRequest) {

        log.info("Subscription request for UUID: {}", vlessUuid);

        VpnConfiguration config = configRepository.findByVlessUuid(vlessUuid)
                .filter(c -> c.getStatus() == ConfigStatus.ACTIVE)
                .orElseThrow(() -> new ConfigNotFoundException("Active config not found"));

        Long userId = config.getUserId();

        boolean isAllowed = deviceSessionService.checkAndRegisterDevice(userId, vlessUuid, httpRequest);

        String subscriptionContent;
        HttpHeaders headers;

        if (!isAllowed) {
            log.warn("Device session blocked: user {} over physical limit", userId);
            subscriptionContent = subscriptionService.generateLimitExceededSubscription(userId);
            headers = headerBuilder.buildLimitExceeded(userId);
        } else {
            subscriptionContent = subscriptionService.generateSubscription(vlessUuid);
            headers = headerBuilder.build(userId);
        }

        return ResponseEntity.ok()
                .headers(headers)
                .header(HttpHeaders.CONTENT_TYPE, "text/plain; charset=utf-8")
                .body(subscriptionContent);
    }

    /**
     * Зашифрованный авто-импорт в Happ через Happ Crypto API.
     * Возвращает HTML-страницу с редиректом на happ://crypt5/...
     * URL совпадает с тем, что строит фронтенд:
     *   subscriptionUrl + "/import-happ"
     *   → /api/v1/subscription/{uuid}/import-happ
     */
    @Public
    @GetMapping(value = "/{vlessUuid}/import-happ", produces = "text/html; charset=utf-8")
    public ResponseEntity<String> importHappEncrypted(@PathVariable UUID vlessUuid) {
        log.info("Encrypted auto-import request for UUID: {}", vlessUuid);

        boolean exists = configRepository.findByVlessUuid(vlessUuid)
                .map(c -> c.getStatus() == ConfigStatus.ACTIVE)
                .orElse(false);

        if (!exists) {
            throw new ConfigNotFoundException("Active config not found");
        }

        String subscriptionUrl = "https://geovp.ru/api/v1/subscription/" + vlessUuid;
        String deepLink;

        try {
            deepLink = encryptHappSubscriptionUrl(subscriptionUrl);
            log.info("Successfully generated encrypted deeplink for {}", vlessUuid);
        } catch (Exception e) {
            log.warn("Happ Crypto API failed, using plain fallback: {}", e.getMessage());
            deepLink = subscriptionUrl.replaceFirst("^https?://", "happ://");
        }

        String html = "<!DOCTYPE html><html>" +
                "<head><meta charset=\"UTF-8\"><title>GeoVPN Import</title></head>" +
                "<body style=\"background:#0a0a0f;color:white;display:flex;justify-content:center;align-items:center;height:100vh;font-family:sans-serif;margin:0;\">" +
                "<div style=\"text-align:center;\">" +
                "<div style=\"font-size:50px;margin-bottom:20px;\">🚀</div>" +
                "<h2>Открываем Happ Proxy...</h2>" +
                "<a href=\"" + deepLink + "\" style=\"display:inline-block;margin-top:20px;padding:15px 30px;background:#ed8936;color:white;text-decoration:none;border-radius:12px;font-weight:bold;\">ОТКРЫТЬ HAPP</a>" +
                "<script>window.location.href = \"" + deepLink + "\";</script>" +
                "</div></body></html>";

        return ResponseEntity.ok(html);
    }


    /**
     * Возвращает зашифрованную ссылку happ://crypt5/... для копирования с фронта.
     */
    @Public
    @GetMapping(value = "/{vlessUuid}/encrypted-link", produces = "text/plain; charset=utf-8")
    public ResponseEntity<String> getEncryptedLink(@PathVariable UUID vlessUuid) {
        String subscriptionUrl = "https://geovp.ru/api/v1/subscription/" + vlessUuid;
        try {
            return ResponseEntity.ok(encryptHappSubscriptionUrl(subscriptionUrl));
        } catch (Exception e) {
            log.warn("Crypto API failed for encrypted-link, returning plain url: {}", e.getMessage());
            return ResponseEntity.ok(subscriptionUrl);
        }
    }

    /**
     * Эндпоинт для автоматического добавления (встраивания) подписки в клиент Happ (legacy).
     */
    @GetMapping("/{vlessUuid}/import/happ")
    public RedirectView importToHapp(@PathVariable UUID vlessUuid) {
        String subscriptionUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/subscription/" + vlessUuid)
                .toUriString();

        String deepLink = subscriptionUrl.replaceFirst("^http(s)?://", "happ://");
        return new RedirectView(deepLink);
    }

    /**
     * Эндпоинт для автоматического добавления в Hiddify
     */
    @GetMapping("/{vlessUuid}/import/hiddify")
    public RedirectView importToHiddify(@PathVariable UUID vlessUuid) {
        String encodedUrl = getEncodedSubscriptionUrl(vlessUuid);
        String deepLink = "hiddify://install-config?url=" + encodedUrl;
        return new RedirectView(deepLink);
    }

    /**
     * Эндпоинт для автоматического добавления в V2Box
     */
    @GetMapping("/{vlessUuid}/import/v2box")
    public RedirectView importToV2Box(@PathVariable UUID vlessUuid) {
        String encodedUrl = getEncodedSubscriptionUrl(vlessUuid);
        String deepLink = "v2box://install-config?url=" + encodedUrl;
        return new RedirectView(deepLink);
    }

    /**
     * Отвязывает физическую сессию устройства (fingerprint из device_sessions).
     * Вызывается фронтендом когда пользователь случайно удалил подписку в Happ
     * и хочет привязать устройство заново.
     *
     * DELETE /api/v1/subscription/{vlessUuid}/session
     */
    @RequireUser
    @DeleteMapping("/{vlessUuid}/session")
    public ResponseEntity<Void> revokeDeviceSession(@PathVariable UUID vlessUuid) {
        Long userId = SecurityContextHolder.getUserId();
        log.info("Revoke session request: userId={}, vlessUuid={}", userId, vlessUuid);

        VpnConfiguration config = configRepository.findByVlessUuid(vlessUuid)
                .filter(c -> c.getStatus() == ConfigStatus.ACTIVE)
                .orElseThrow(() -> new ConfigNotFoundException("Active config not found"));

        if (!config.getUserId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }

        deviceSessionService.revokeSessionByVlessUuid(userId, vlessUuid);
        return ResponseEntity.noContent().build();
    }

    /**
     * Вспомогательный метод для получения закодированного URL самой подписки (без суффиксов import)
     */
    private String getEncodedSubscriptionUrl(UUID vlessUuid) {
        String subscriptionUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/subscription/" + vlessUuid)
                .toUriString();

        if (subscriptionUrl.startsWith("http://") && !subscriptionUrl.contains("localhost")) {
            subscriptionUrl = subscriptionUrl.replaceFirst("http://", "https://");
        }

        return URLEncoder.encode(subscriptionUrl, StandardCharsets.UTF_8);
    }

    /**
     * Шифрует URL через Happ Crypto API → happ://crypt5/...
     * Зашифрованная подписка скрывает адреса серверов от пользователя.
     */
    private String encryptHappSubscriptionUrl(String url) throws Exception {
        String requestBody = "{\"url\":\"" + url + "\"}";

        java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("https://crypto.happ.su/api-v2.php"))
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(5))
                .build();

        java.net.http.HttpResponse<String> response = client.send(
                request, java.net.http.HttpResponse.BodyHandlers.ofString());

        String body = response.body().trim();

        if (body.startsWith("{")) {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(body);
            if (node.has("encrypted_link")) {
                return node.get("encrypted_link").asText();
            }
        }

        if (body.startsWith("happ://")) return body;

        throw new RuntimeException("Happ Crypto API error: " + body);
    }
}