package com.vpn.config.generator;

import com.vpn.common.dto.response.VpnConfigResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Генератор подписки для VPN клиентов
 * Поддерживает: V2Box, Happ, V2RayNG, NekoBox
 *
 * Формат: base64(vless://link1\nvless://link2\n...)
 */
@Slf4j
@Component
public class SubscriptionBuilder {

    /**
     * Маппинг кодов стран на флаги
     */
    private static final java.util.Map<String, String> COUNTRY_FLAGS = java.util.Map.of(
            "RU", "🇷🇺",
            "NL", "🇳🇱",
            "DE", "🇩🇪",
            "FI", "🇫🇮",
            "PL", "🇵🇱",
            "SE", "🇸🇪",
            "EE", "🇪🇪",
            "FR", "🇫🇷",
            "GB", "🇬🇧",
            "US", "🇺🇸"
    );

    /**
     * Построить base64 подписку из списка ссылок
     * Сортировка: сначала антиглушилки, потом по странам
     */
    public String buildSubscription(List<VpnConfigResponse.ServerConfig> configs) {
        String links = configs.stream()
                .sorted(Comparator
                        .comparing((VpnConfigResponse.ServerConfig c) -> !c.getIsRelay()) // relay первыми
                        .thenComparingInt(c -> c.getAvgLatencyMs() != null ? c.getAvgLatencyMs() : 999))
                .map(VpnConfigResponse.ServerConfig::getVlessLink)
                .collect(Collectors.joining("\n"));

        String encoded = Base64.getEncoder().encodeToString(links.getBytes());
        log.info("Built subscription with {} servers", configs.size());

        return encoded;
    }

    /**
     * Сгенерировать имя сервера для отображения в клиенте
     * Формат: "🇷🇺 Антиглушилка #1 4G/LTE" или "🇳🇱 Нидерланды 🚀"
     */
    public String buildServerDisplayName(
            String countryCode,
            String serverType,
            int serverIndex,
            String location
    ) {
        String flag = COUNTRY_FLAGS.getOrDefault(countryCode, "🌐");

        return switch (serverType) {
            case "ANTIGLUSH" ->
                    flag + " Антиглушилка #" + serverIndex + " 4G/LTE";
            case "WHITELIST" ->
                    flag + " Белые списки #" + serverIndex;
            default ->
                    flag + " " + location + " 🚀";
        };
    }

    /**
     * Получить эмодзи флага по коду страны
     */
    public String getCountryFlag(String countryCode) {
        return COUNTRY_FLAGS.getOrDefault(countryCode, "🌐");
    }
}
