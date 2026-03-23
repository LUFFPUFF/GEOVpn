package com.vpn.config.service;

import com.vpn.common.dto.ServerDto;
import com.vpn.common.dto.enums.ConfigStatus;
import com.vpn.config.domain.entity.VpnConfiguration;
import com.vpn.config.domain.valueobject.ServerAddress;
import com.vpn.config.exception.ConfigNotFoundException;
import com.vpn.config.generator.RealityConfigGenerator;
import com.vpn.config.generator.VlessLinkBuilder;
import com.vpn.config.repository.VpnConfigurationRepository;
import com.vpn.config.service.interf.ServerSelectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Сервис подписок — один URL возвращает все серверы
 * Клиент (V2Box, V2RayNG, Hiddify) периодически обновляет список серверов автоматически
 *
 * Формат: base64(vless://...\nvless://...\nvless://...)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final VpnConfigurationRepository configRepository;
    private final ServerSelectionService serverSelectionService;
    private final VlessLinkBuilder vlessLinkBuilder;
    private final RealityConfigGenerator realityGenerator;

    @Value("${vpn.relay.ru.ip:}")
    private String ruRelayIp;

    /**
     * Генерирует содержимое подписки по UUID пользователя
     * Возвращает base64 строку со всеми VLESS ссылками
     */
    public String generateSubscription(UUID vlessUuid) {
        log.info("Generating subscription for UUID: {}", vlessUuid);

        VpnConfiguration config = configRepository.findByVlessUuid(vlessUuid)
                .orElseThrow(() -> new ConfigNotFoundException(
                        "Config not found with UUID: " + vlessUuid));

        if (config.getStatus() != ConfigStatus.ACTIVE) {
            throw new ConfigNotFoundException("Config is not active: " + vlessUuid);
        }

        List<ServerDto> allServers = serverSelectionService.getAllActiveServers();
        List<String> links = new ArrayList<>();

        for (ServerDto server : allServers) {
            try {
                String pbk = server.getRealityPublicKey() != null
                        ? server.getRealityPublicKey()
                        : realityGenerator.generatePublicKey();

                String sid = server.getRealityShortId() != null
                        ? server.getRealityShortId()
                        : realityGenerator.generateShortId();

                if (ruRelayIp != null && !ruRelayIp.isEmpty()) {
                    String relayLink = vlessLinkBuilder.buildRelayVlessLink(
                            vlessUuid,
                            vlessUuid.toString(),
                            ruRelayIp,
                            server.getPort(),
                            pbk,
                            sid,
                            "🇷🇺 " + server.getName() + " | Антиглуш"
                    );
                    links.add(relayLink);
                }

                String directLink = vlessLinkBuilder.buildVlessLink(
                        vlessUuid,
                        vlessUuid.toString(),
                        new ServerAddress(server.getIpAddress()),
                        server.getPort(),
                        "🌍 " + server.getName() + " | Direct",
                        pbk,
                        sid
                );
                links.add(directLink);

            } catch (Exception e) {
                log.error("Failed to generate link for server: {}", server.getName(), e);
            }
        }

        String combined = String.join("\n", links);
        String encoded = Base64.getEncoder()
                .encodeToString(combined.getBytes(StandardCharsets.UTF_8));

        log.info("Generated subscription with {} links for UUID: {}",
                links.size(), vlessUuid);

        return encoded;
    }


}
