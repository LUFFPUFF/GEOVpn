package com.vpn.config.service.rules;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vpn.common.dto.RoutingRule;
import com.vpn.common.dto.ServerDto;
import com.vpn.config.domain.entity.VpnConfiguration;
import com.vpn.config.repository.VpnConfigurationRepository;
import com.vpn.config.service.interf.ServerSelectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Генератор полного Xray JSON конфига
 *
 * Используется для:
 * 1. Скачивания конфига пользователем (import в v2rayNG)
 * 2. QR код с полным конфигом
 * 3. API endpoint для клиентов
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class XrayConfigGenerator {

    private final VpnConfigurationRepository configRepository;
    private final ServerSelectionService serverSelectionService;
    private final RoutingRulesGenerator routingRulesGenerator;
    private final ObjectMapper objectMapper;

    /**
     * Генерирует полный Xray JSON конфиг для устройства
     *
     * @param deviceId ID устройства
     * @return JSON строка с конфигом
     */
    public String generateFullConfig(Long deviceId) {
        log.info("Генерация полного Xray конфига для device: {}", deviceId);

        VpnConfiguration vpnConfig = configRepository
                .findByDeviceIdAndStatus(deviceId, com.vpn.common.dto.enums.ConfigStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("Config not found for device: " + deviceId));

        ServerDto server = serverSelectionService.getAllActiveServers()
                .stream()
                .filter(s -> s.getId().equals(vpnConfig.getServerId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Server not found: " + vpnConfig.getServerId()));

        List<RoutingRule> rules = routingRulesGenerator.generateRoutingRules();

        Map<String, Object> fullConfig = buildXrayConfig(vpnConfig, server, rules);

        try {
            String json = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(fullConfig);

            log.info("Конфиг сгенерирован, размер: {} байт", json.length());

            return json;

        } catch (Exception e) {
            log.error("Ошибка сериализации конфига", e);
            throw new RuntimeException("Failed to generate config JSON", e);
        }
    }

    /**
     * Собирает полный Xray конфиг в виде Map
     */
    private Map<String, Object> buildXrayConfig(
            VpnConfiguration vpnConfig,
            ServerDto server,
            List<RoutingRule> rules
    ) {
        Map<String, Object> config = new LinkedHashMap<>();

        config.put("log", Map.of(
                "loglevel", "warning"
        ));

        config.put("inbounds", List.of(
                Map.of(
                        "port", 10808,
                        "protocol", "socks",
                        "settings", Map.of(
                                "udp", true
                        ),
                        "tag", "socks-in"
                ),
                Map.of(
                        "port", 10809,
                        "protocol", "http",
                        "tag", "http-in"
                )
        ));

        List<Map<String, Object>> outbounds = new ArrayList<>();

        outbounds.add(buildVlessOutbound(vpnConfig, server));

        outbounds.add(Map.of(
                "protocol", "freedom",
                "tag", "direct",
                "settings", Map.of(
                        "domainStrategy", "UseIPv4"
                )
        ));

        outbounds.add(Map.of(
                "protocol", "blackhole",
                "tag", "block"
        ));

        config.put("outbounds", outbounds);

        config.put("routing", Map.of(
                "domainStrategy", "IPIfNonMatch",
                "rules", rules
        ));

        config.put("dns", Map.of(
                "servers", List.of(
                        Map.of(
                                "address", "https://1.1.1.1/dns-query",
                                "domains", List.of("geosite:geolocation-!cn")
                        ),
                        Map.of(
                                "address", "77.88.8.8",
                                "domains", List.of("geosite:category-ru")
                        ),
                        "1.1.1.1"
                ),
                "queryStrategy", "UseIPv4"
        ));

        return config;
    }

    /**
     * Создает VLESS outbound конфиг
     */
    private Map<String, Object> buildVlessOutbound(
            VpnConfiguration vpnConfig,
            ServerDto server
    ) {
        return Map.of(
                "protocol", "vless",
                "tag", "proxy",
                "settings", Map.of(
                        "vnext", List.of(
                                Map.of(
                                        "address", server.getIpAddress(),
                                        "port", server.getPort(),
                                        "users", List.of(
                                                Map.of(
                                                        "id", vpnConfig.getVlessUuid().toString(),
                                                        "encryption", "none",
                                                        "flow", "xtls-rprx-vision"
                                                )
                                        )
                                )
                        )
                ),
                "streamSettings", Map.of(
                        "network", "tcp",
                        "security", "reality",
                        "realitySettings", Map.of(
                                "show", false,
                                "fingerprint", "chrome",
                                "serverName", server.getRealitySni() != null
                                        ? server.getRealitySni()
                                        : "www.microsoft.com",
                                "publicKey", server.getRealityPublicKey(),
                                "shortId", server.getRealityShortId(),
                                "spiderX", ""
                        ),
                        "tcpSettings", Map.of(
                                "header", Map.of("type", "none")
                        )
                )
        );
    }
}
