package com.vpn.config.service;

import com.vpn.common.dto.ServerDto;
import com.vpn.config.domain.entity.VpnConfiguration;
import com.vpn.config.domain.valueobject.ServerAddress;
import com.vpn.config.domain.valueobject.StoredVpnLinks;
import com.vpn.config.generator.VlessLinkBuilder;
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
 *   2. Прямые VLESS-ссылки по каждому активному VPS     ← основные (VLESS + TCP + Reality)
 *   3. Hysteria2 по каждому не-RU серверу               ← UDP fallback
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VpnLinksBuilder {

    private final VlessLinkBuilder         vlessLinkBuilder;
    private final Hysteria2ConfigGenerator hysteria2Generator;
    private final ServerSelectionService   serverSelectionService;

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
                        + " " + ("RU".equalsIgnoreCase(server.getCountryCode()) ? "" : "🚀 ")
                        + server.getName();

                String link = vlessLinkBuilder.buildVlessLinkCustom(
                        uuid,
                        new ServerAddress(server.getIpAddress()),
                        server.getPort(),
                        displayName,
                        server.getRealityPublicKey(),
                        server.getRealityShortId(),
                        server.getRealitySni() != null ? server.getRealitySni() : "www.microsoft.com",
                        "chrome",
                        null
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

                if (!"RU".equalsIgnoreCase(server.getCountryCode())) {
                    String subdomain = server.getCountryCode().toLowerCase();
                    String domain = subdomain + ".geovp.ru";

                    String actualPath = switch (server.getCountryCode().toUpperCase()) {
                        case "SE" -> "/swedish-cl";
                        case "FI" -> "/finland-cl";
                        case "FR" -> "/france-cl";
                        default -> "/" + subdomain + "-cl";
                    };

                    String fallbackName = countryEmoji(server.getCountryCode()) + " 🚀 " + server.getName() + "-2";

                    String fallbackLink = "vless://" + uuid.toString() + "@" + domain + ":2053" +
                            "?type=ws&encryption=none&path=" + java.net.URLEncoder.encode(actualPath, java.nio.charset.StandardCharsets.UTF_8) +
                            "&host=" + domain + "&security=tls&fp=chrome&alpn=http/1.1&sni=" + domain +
                            "#" + java.net.URLEncoder.encode(fallbackName, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");

                    result.add(StoredVpnLinks.DirectLink.builder()
                            .serverId(server.getId())
                            .serverName(server.getName() + "-2")
                            .countryCode(server.getCountryCode())
                            .link(fallbackLink)
                            .avgLatencyMs(server.getAvgLatencyMs())
                            .healthScore(server.getHealthScore())
                            .displayName(fallbackName)
                            .build());
                }

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
        List<String> links = new ArrayList<>();
        for (ServerDto server : servers) {
            if ("RU".equalsIgnoreCase(server.getCountryCode())) continue;

            try {
                String subdomain = server.getCountryCode().toLowerCase();
                String domain = subdomain + ".geovp.ru";
                String title = countryEmoji(server.getCountryCode()) + " 🚀 " + server.getName() + " | HY2";

                String hy2Link = "hysteria2://25bfe47e@" + domain + ":443" +
                        "?sni=" + domain +
                        "#" + java.net.URLEncoder.encode(title, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");

                links.add(hy2Link);
            } catch (Exception e) {
                log.warn("Failed to build HY2 link for {}: {}", server.getName(), e.getMessage());
            }
        }
        return links;
    }

    private String buildRelayDisplayName(ServerDto relay, int index) {
        return "🇪🇺 Антиглушилка #" + index + " | LTE 📶";
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