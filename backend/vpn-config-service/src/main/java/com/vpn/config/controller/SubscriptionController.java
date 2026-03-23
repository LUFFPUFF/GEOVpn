package com.vpn.config.controller;

import com.vpn.common.security.annotations.Public;
import com.vpn.common.security.annotations.RequireUser;
import com.vpn.config.domain.entity.VpnConfiguration;
import com.vpn.common.dto.ServerDto;
import com.vpn.config.repository.VpnConfigurationRepository;
import com.vpn.config.service.SubscriptionService;
import com.vpn.config.service.interf.ServerSelectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    /**
     * Эндпоинт подписки для VPN клиентов
     * GET /api/v1/subscription/{vlessUuid}
     *
     * Клиент добавляет этот URL — все серверы обновляются автоматически
     */
    @GetMapping("/{vlessUuid}")
    @RequireUser
    public ResponseEntity<String> getSubscription(@PathVariable UUID vlessUuid) {
        log.info("Subscription request for UUID: {}", vlessUuid);

        String subscriptionContent = subscriptionService.generateSubscription(vlessUuid);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/plain; charset=utf-8")
                .header("subscription-userinfo", "upload=0; download=0; total=10737418240; expire=0")
                .header("profile-title", "GeoVPN")
                .header("profile-update-interval", "24")
                .body(subscriptionContent);
    }
}
