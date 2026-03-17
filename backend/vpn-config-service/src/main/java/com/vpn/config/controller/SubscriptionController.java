package com.vpn.config.controller;

import com.vpn.common.security.annotations.Public;
import com.vpn.config.domain.entity.VpnConfiguration;
import com.vpn.common.dto.ServerDto;
import com.vpn.config.repository.VpnConfigurationRepository;
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
@RequestMapping("/sub")
@RequiredArgsConstructor
public class SubscriptionController {

    private final VpnConfigurationRepository configRepository;
    private final ServerSelectionService serverSelectionService;

    @GetMapping("/{vlessUuid}")
    @Public
    public ResponseEntity<String> getSubscription(@PathVariable String vlessUuid) {

        String cleanUuid = vlessUuid.trim();
        log.info("📥 Запрошена подписка для UUID: {}", vlessUuid);

        UUID uuid;
        try {
            uuid = UUID.fromString(cleanUuid);
        } catch (IllegalArgumentException e) {
            log.warn("❌ Неверный формат UUID: {}", cleanUuid);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid UUID format");
        }
        VpnConfiguration primaryConfig = configRepository.findByVlessUuid(uuid).orElse(null);
        if (primaryConfig == null || !primaryConfig.isActive()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Subscription not found or inactive");
        }

        ServerDto server = serverSelectionService.getAllActiveServers().stream()
                .filter(s -> s.getId().equals(primaryConfig.getServerId()))
                .findFirst()
                .orElse(null);

        if (server == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Server is offline");
        }

        StringBuilder payloadBuilder = new StringBuilder();

        String vlessLink = String.format(
                "vless://%s@%s:%d?security=reality&sni=%s&fp=chrome&pbk=%s&sid=%s&type=tcp&flow=xtls-rprx-vision#%s-VLESS",
                primaryConfig.getVlessUuid(),
                server.getIpAddress(),
                server.getPort(),
                server.getRealitySni(),
                server.getRealityPublicKey(),
                server.getRealityShortId(),
                server.getName()
        );
        payloadBuilder.append(vlessLink).append("\n");

        String hysteriaLink = String.format(
                "hysteria2://%s@%s:8443?sni=%s&insecure=1#%s-Hysteria2(Anti-Block)",
                primaryConfig.getVlessUuid(),
                server.getIpAddress(),
                server.getRealitySni(),
                server.getName()
        );
        payloadBuilder.append(hysteriaLink).append("\n");

        String base64Payload = Base64.getEncoder().encodeToString(payloadBuilder.toString().getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.set("profile-update-interval", "12");
        headers.set("profile-title", "GeoVPN Premium");
        headers.set("subscription-userinfo", "upload=0; download=0; total=10737418240; expire=0");

        return new ResponseEntity<>(base64Payload, headers, HttpStatus.OK);
    }
}
