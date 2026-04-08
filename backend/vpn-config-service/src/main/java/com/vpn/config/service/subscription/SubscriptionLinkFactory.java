package com.vpn.config.service.subscription;

import com.vpn.common.dto.ServerDto;
import com.vpn.config.config.VpnConfigProperties;
import com.vpn.config.domain.valueobject.ServerAddress;
import com.vpn.config.generator.RealityConfigGenerator;
import com.vpn.config.generator.VlessLinkBuilder;
import com.vpn.config.generator.hysteria2.Hysteria2ConfigGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Генерирует все ссылки для одного сервера:
 * — VLESS Direct (прямое подключение)
 * — VLESS Relay (через RU антиглушилку)
 * — Hysteria2 (UDP fallback для мобильных сетей)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionLinkFactory {

    private final VlessLinkBuilder vlessLinkBuilder;
    private final RealityConfigGenerator realityGenerator;
    private final Hysteria2ConfigGenerator hysteria2Generator;
    private final VpnConfigProperties configProperties;

    @Value("${vpn.relay.ru.ip:}")
    private String ruRelayIp;

    public String buildRelayLink(UUID uuid) {
        String name = "Антиглушилка | Европа LTE";

        String link = vlessLinkBuilder.buildVlessLinkCustom(
                uuid,
                new ServerAddress(ruRelayIp),
                443,
                name,
                "sfyqJlJ5awfoPyFtEEuqmiywFWRJyR_nJzELeB1hckQ",
                "b0f3f1e7a86291",
                "eh.vk.com",
                "chrome"
        );

        String finalLink = link + "&flow=xtls-rprx-vision&type=tcp&countryCode=EU";

        return encodeName(finalLink) + "?serverDescription=" + b64("LTE | 4G Антиглушилка");
    }

    public String buildDirectVlessLink(UUID uuid, ServerDto server) {
        String pbk = server.getRealityPublicKey() != null ? server.getRealityPublicKey() : realityGenerator.generatePublicKey();
        String sid = server.getRealityShortId() != null ? server.getRealityShortId() : realityGenerator.generateShortId();

        String directLink = "";
        try {
            directLink = vlessLinkBuilder.buildVlessLinkCustom(
                    uuid, new ServerAddress(server.getIpAddress()), server.getPort(),
                    countryEmoji(server.getCountryCode()) + " " + server.getName(),
                    pbk, sid, server.getRealitySni(), "chrome"
            );
        } catch (Exception ignored) {}

        return directLink;
    }

    public String buildHy2Link(ServerDto server) {
        String hy2Link = "";

        if (hysteria2Generator.isHysteria2Configured()) {
            try {
                String title = countryEmoji(server.getCountryCode()) + " " + server.getName() + " | HY2";
                hy2Link = hysteria2Generator.buildHysteria2Link(server, title);
            } catch (Exception ignored) {}
        }

        return hy2Link;
    }

    public List<String> buildLinksForServer(UUID uuid, ServerDto server, int deviceIndex) {
        List<String> links = new ArrayList<>();
        String pbk = server.getRealityPublicKey() != null ? server.getRealityPublicKey() : realityGenerator.generatePublicKey();
        String sid = server.getRealityShortId() != null ? server.getRealityShortId() : realityGenerator.generateShortId();

        String deviceSuffix = " (Устр. " + deviceIndex + ")";

        String descLte = encodeBase64("LTE | 4G");
        String descVless = encodeBase64("VLESS | JSON");
        String descHy2 = encodeBase64("HY2 | UDP");

        if (ruRelayIp != null && !ruRelayIp.isEmpty()) {
            try {
                String relayLink = vlessLinkBuilder.buildVlessLinkCustom(
                        uuid, new ServerAddress(ruRelayIp), server.getPort(),
                        "🇷🇺 " + server.getName() + " | LTE" + deviceSuffix,
                        pbk, sid, configProperties.getVless().getRelaySni(), "chrome"
                );
                links.add(relayLink + "?serverDescription=" + descLte);
            } catch (Exception ignored) {}
        }

        try {
            String directLink = vlessLinkBuilder.buildVlessLinkCustom(
                    uuid, new ServerAddress(server.getIpAddress()), server.getPort(),
                    countryEmoji(server.getCountryCode()) + " " + server.getName() + deviceSuffix,
                    pbk, sid, server.getRealitySni(), "chrome"
            );
            links.add(directLink + "?serverDescription=" + descVless);
        } catch (Exception ignored) {}

        if (hysteria2Generator.isHysteria2Configured()) {
            try {
                String title = countryEmoji(server.getCountryCode()) + " " + server.getName() + " | HY2" + deviceSuffix;
                String hy2Link = hysteria2Generator.buildHysteria2Link(server, title);
                links.add(hy2Link + "?serverDescription=" + descHy2);
            } catch (Exception ignored) {}
        }

        return links;
    }

    private String injectParams(String link, String params) {
        return link.replace("#", params + "#");
    }

    private String b64(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    private String encodeName(String link) {
        return link.replace("+", "%20");
    }

    public String buildPaymentLink() {
        return "vless://000@1.1.1.1:443?encryption=none&security=none#🌐%20Личный%20кабинет%20/%20Оплата";
    }

    private String encodeBase64(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8))
                .replace("\n", "").replace("\r", "");
    }

    private String countryEmoji(String code) {
        if (code == null) return "🌐";
        return switch (code.toUpperCase()) {
            case "NL" -> "🇳🇱"; case "DE" -> "🇩🇪"; case "FI" -> "🇫🇮";
            case "PL" -> "🇵🇱"; case "EE" -> "🇪🇪"; case "SE" -> "🇸🇪";
            case "FR" -> "🇫🇷"; case "GB" -> "🇬🇧"; case "US" -> "🇺🇸";
            case "RU" -> "🇷🇺"; default   -> "🌐";
        };
    }
}
