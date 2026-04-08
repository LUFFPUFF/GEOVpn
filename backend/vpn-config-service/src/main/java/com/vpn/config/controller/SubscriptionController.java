package com.vpn.config.controller;

import com.vpn.common.security.annotations.RequireUser;
import com.vpn.config.domain.entity.VpnConfiguration;
import com.vpn.config.repository.VpnConfigurationRepository;
import com.vpn.config.service.DeviceLimitService;
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
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final SubscriptionHeaderBuilder headerBuilder;
    private final VpnConfigurationRepository configRepository;
    private final DeviceLimitService deviceLimitService;
    private final DeviceSessionService deviceSessionService;

    /**
     * Эндпоинт подписки для VPN-клиентов
     * GET /api/v1/subscription/{vlessUuid}
     */
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
     * Эндпоинт для автоматического добавления (встраивания) подписки в клиент Happ.
     */
    @GetMapping("/{vlessUuid}/import/happ")
    public RedirectView importToHapp(@PathVariable UUID vlessUuid) {
        String encodedUrl = getEncodedSubscriptionUrl(vlessUuid);
        String deepLink = "happ://add-sub?url=" + encodedUrl;
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
}
