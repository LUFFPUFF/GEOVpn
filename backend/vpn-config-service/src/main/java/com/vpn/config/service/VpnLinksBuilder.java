package com.vpn.config.service;

import com.vpn.common.dto.ServerDto;
import com.vpn.config.domain.entity.VpnConfiguration;
import com.vpn.config.domain.valueobject.ServerAddress;
import com.vpn.config.domain.valueobject.StoredVpnLinks;
import com.vpn.config.generator.VlessLinkBuilder;
import com.vpn.config.generator.VlessLinkBuilder.XhttpParams;
import com.vpn.config.generator.hysteria2.Hysteria2ConfigGenerator;
import com.vpn.config.service.interf.ServerSelectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Генерирует все VPN-ссылки для конфигурации и сохраняет их в entity.
 *
 * Вызывается один раз при createConfig / regenerateConfig.
 * При запросе подписки ссылки берутся из БД (JSONB).
 *
 * Порядок ссылок:
 *   1. Relay-ссылки (сортировка по relay_priority ASC)  ← для LTE/глушения
 *   2. Прямые VLESS-ссылки по каждому активному VPS     ← основные
 *   3. Hysteria2 по каждому не-RU серверу               ← UDP fallback
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VpnLinksBuilder {

    private final VlessLinkBuilder         vlessLinkBuilder;
    private final Hysteria2ConfigGenerator hysteria2Generator;
    private final ServerSelectionService   serverSelectionService;

    private static final String DEFAULT_TRANSPORT = "xhttp";
    private static final String DEFAULT_XHTTP_PATH = "/api/v1/data";
    private static final String DEFAULT_XHTTP_MODE = "stream-one";
    private static final String DEFAULT_XHTTP_PADDING = "100-1000";

    /**
     * Строит все ссылки и записывает их в переданный config (без save).
     * Вызывающий код должен сам сохранить entity после вызова.
     */
    public void buildAndStore(VpnConfiguration config) {
        UUID uuid = config.getVlessUuid();
        List<ServerDto> allServers = serverSelectionService.getAllActiveServers();

        List<ServerDto> relayServers = allServers.stream()
                .filter(ServerDto::isRelay)
                .sorted(Comparator.comparingInt(ServerDto::getRelayPriority))
                .toList();
        List<StoredVpnLinks.RelayLink> relayLinks = buildRelayLinks(uuid, relayServers);

        List<ServerDto> directServers = allServers.stream()
                .filter(s -> !s.isRelay())
                .toList();
        List<StoredVpnLinks.DirectLink> directLinks = buildDirectLinks(uuid, directServers);

        List<String> hy2Links = buildHy2Links(directServers, uuid);

        config.storeLinks(directLinks, relayLinks, hy2Links);
    }

    private List<StoredVpnLinks.RelayLink> buildRelayLinks(UUID uuid, List<ServerDto> relayServers) {
        List<StoredVpnLinks.RelayLink> result = new ArrayList<>();

        for (ServerDto relay : relayServers) {
            try {
                String link = vlessLinkBuilder.buildVlessLinkCustom(
                        uuid,
                        new ServerAddress(relay.getIpAddress()),
                        relay.getPort(),
                        buildRelayDisplayName(relay, result.size() + 1),
                        relay.getRelayPublicKey() != null ? relay.getRelayPublicKey() : relay.getRealityPublicKey(),
                        relay.getRelayShortId() != null ? relay.getRelayShortId() : relay.getRealityShortId(),
                        relay.getRelaySni() != null ? relay.getRelaySni() : "eh.vk.com",
                        "chrome",
                        null
                );

                if (relay.getCountryCode() != null) {
                    int hashIdx = link.indexOf('#');
                    if (hashIdx != -1) {
                        link = link.substring(0, hashIdx) + "&countryCode=" + relay.getCountryCode() + link.substring(hashIdx);
                    }
                }

                result.add(StoredVpnLinks.RelayLink.builder()
                        .serverId(relay.getId())
                        .serverName(relay.getName())
                        .countryCode(relay.getCountryCode())
                        .link(link)
                        .relayPriority(relay.getRelayPriority())
                        .description("LTE | 4G Антиглушилка #" + (result.size() + 1))
                        .build());

            } catch (Exception e) {
                log.warn("Failed to build relay link: {}", e.getMessage());
            }
        }
        return result;
    }

    private List<StoredVpnLinks.DirectLink> buildDirectLinks(UUID uuid, List<ServerDto> directServers) {
        List<StoredVpnLinks.DirectLink> result = new ArrayList<>();

        for (ServerDto server : directServers) {
            try {
                String displayName = countryEmoji(server.getCountryCode())
                        + " " + server.getName();

                XhttpParams xhttp = resolveXhttpParams(server);

                String link = vlessLinkBuilder.buildVlessLinkCustom(
                        uuid,
                        new ServerAddress(server.getIpAddress()),
                        server.getPort(),
                        displayName,
                        server.getRealityPublicKey(),
                        server.getRealityShortId(),
                        server.getRealitySni() != null ? server.getRealitySni() : "www.microsoft.com",
                        "chrome",
                        xhttp
                );

                result.add(StoredVpnLinks.DirectLink.builder()
                        .serverId(server.getId())
                        .serverName(server.getName())
                        .countryCode(server.getCountryCode())
                        .link(link)
                        .avgLatencyMs(server.getAvgLatencyMs())
                        .healthScore(server.getHealthScore())
                        .displayName(displayName)
                        .build());

            } catch (Exception e) {
                log.warn("Failed to build direct link for server={}: {}", server.getName(), e.getMessage());
            }
        }

        return result;
    }

    /**
     * Строит HY2-ссылки для всех прямых не-RU серверов с настроенным Hysteria2.
     */
    private List<String> buildHy2Links(List<ServerDto> servers, UUID uuid) {
        if (!hysteria2Generator.isHysteria2Configured()) return List.of();

        List<String> links = new ArrayList<>();
        for (ServerDto server : servers) {
            if ("RU".equalsIgnoreCase(server.getCountryCode())) continue;

            try {
                String title = countryEmoji(server.getCountryCode()) + " " + server.getName() + " | HY2";
                links.add(hysteria2Generator.buildHysteria2Link(server, title));
            } catch (Exception e) {
                log.warn("Failed to build HY2 link for {}: {}", server.getName(), e.getMessage());
            }
        }
        return links;
    }

    /**
     * Определяет XHTTP-параметры для сервера.
     * Если у ServerDto транспорт != "xhttp" (или поле null) — возвращает null (→ TCP).
     *
     * Предполагается, что ServerDto содержит поля:
     *   - transportType  (String): "tcp" | "xhttp"
     *   - xhttpPath      (String): например "/api/v1/data"
     *   - xhttpMode      (String): например "stream-one"
     *   - xhttpPadding   (String): например "100-1000"
     */
    private XhttpParams resolveXhttpParams(ServerDto server) {
        return XhttpParams.builder()
                .path(DEFAULT_XHTTP_PATH)
                .mode(DEFAULT_XHTTP_MODE)
                .paddingBytes(DEFAULT_XHTTP_PADDING)
                .build();
    }

    /**
     * Сервер считается RU, если countryCode == "RU" или isRelay == true.
     */
    private boolean isRuServer(ServerDto server) {
        return "RU".equalsIgnoreCase(server.getCountryCode()) || server.isRelay();
    }

    private String buildRelayDisplayName(ServerDto relay, int index) {
        String emoji = "🇷🇺";
        if ("UA".equalsIgnoreCase(relay.getCountryCode())) emoji = "🇺🇦";
        if ("KZ".equalsIgnoreCase(relay.getCountryCode())) emoji = "🇰🇿";

        return emoji + " Антиглушилка #" + index + " | LTE 📶";
    }

    private String countryEmoji(String code) {
        if (code == null) return "🌐";
        return switch (code.toUpperCase()) {
            case "NL" -> "🇳🇱"; case "DE" -> "🇩🇪"; case "FI" -> "🇫🇮";
            case "PL" -> "🇵🇱"; case "EE" -> "🇪🇪"; case "SE" -> "🇸🇪";
            case "FR" -> "🇫🇷"; case "GB" -> "🇬🇧"; case "US" -> "🇺🇸";
            case "LV" -> "🇱🇻"; case "LT" -> "🇱🇹"; case "RU" -> "🇷🇺";
            default   -> "🌐";
        };
    }
}