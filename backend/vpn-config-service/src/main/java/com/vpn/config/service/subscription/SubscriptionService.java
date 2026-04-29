package com.vpn.config.service.subscription;

import com.vpn.common.dto.enums.ConfigStatus;
import com.vpn.common.dto.enums.SubscriptionType;
import com.vpn.config.client.UserServiceClient;
import com.vpn.config.domain.entity.VpnConfiguration;
import com.vpn.config.domain.valueobject.StoredVpnLinks;
import com.vpn.config.exception.ConfigNotFoundException;
import com.vpn.config.repository.VpnConfigurationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Генерирует base64-подписку для VPN клиентов (Happ, Hiddify, V2Box).
 *
 * Ключевая логика:
 * Ссылки НЕ пересчитываются при каждом запросе — они берутся
 * из JSONB-полей vpn_configurations (vless_links_json, relay_links_json, hy2_link),
 * куда были записаны единожды при createConfig/regenerateConfig.
 *
 * Структура подписки:
 *   #profile-title:  …
 *   #announce:       …  (баннер)
 *
 *   [для каждого устройства пользователя:]
 *   vless://relay1…     ← антиглушилки первыми
 *   vless://relay2…
 *   vless://direct-FI…  ← прямые серверы
 *   vless://direct-NL…
 *   hy2://…             ← UDP fallback
 *
 *   vless://payment…    ← ссылка ЛК
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final VpnConfigurationRepository configRepository;
    private final SubscriptionBanner         banner;
    private final UserServiceClient         userServiceClient;

    /**
     * Генерирует полную подписку для пользователя по любому из его UUID.
     *
     * @param vlessUuid  UUID одного из активных устройств пользователя
     * @return base64-строка с подпиской
     */
    public String generateSubscription(UUID vlessUuid) {
        VpnConfiguration rootConfig = configRepository.findByVlessUuid(vlessUuid)
                .filter(c -> c.getStatus() == ConfigStatus.ACTIVE)
                .orElseThrow(() -> new ConfigNotFoundException("Active config not found: " + vlessUuid));

        Long userId = rootConfig.getUserId();
        List<VpnConfiguration> userConfigs = configRepository
                .findByUserIdAndStatus(userId, ConfigStatus.ACTIVE);

        var userDto = userServiceClient.getUserByTelegramId(userId).getData();

        List<String> lines = new ArrayList<>();

        lines.add("#profile-title: base64:" + b64("GeoVPN | " + userDto.getSubscriptionType().name()));

        long unixExpire = userDto.getSubscriptionExpiresAt() != null
                ? java.time.ZonedDateTime.parse(userDto.getSubscriptionExpiresAt() + "Z").toEpochSecond()
                : 0;

        lines.add("#subscription-userinfo: upload=0; download=0; total=0; expire=" + unixExpire);

        assert userDto.getSubscriptionExpiresAt() != null;
        lines.add("#announce: base64:" + b64("Ваш ключ GeoVPN активен до " + userDto.getSubscriptionExpiresAt().toLocalDate()));
        lines.add("#support-url: https://t.me/geovp_support_bot");
        lines.add("");

        for (VpnConfiguration deviceConfig : userConfigs) {
            appendDeviceLinks(lines, deviceConfig);
        }

        lines.add(paymentLink());
        String raw = String.join("\n", lines);
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Подписка с блокировкой (лимит устройств превышен).
     * Показывает инструкцию и кнопку апгрейда вместо рабочих серверов.
     */
    public String generateLimitExceededSubscription(Long userId) {
        List<String> lines = new ArrayList<>();

        lines.add("#profile-title: base64:" + b64("⚠️ GeoVPN | ЛИМИТ"));
        lines.add("#announce: base64:" + b64(buildLimitBanner()));
        lines.add("");

        lines.add("#sub-info-color: red");
        lines.add("#sub-info-text: " + b64("🚫 Лимит устройств исчерпан! Нажмите 'Улучшить' для апгрейда."));
        lines.add("#sub-info-button-text: " + b64("🚀 Улучшить тариф"));
        lines.add("#sub-info-button-link: https://t.me/geovp_bot?start=upgrade");
        lines.add("");

        lines.add(infoLink("1. Лимит устройств исчерпан",
                "Перейдите в бот и удалите старые устройства"));
        lines.add(infoLink("2. Удалите неиспользуемые устройства",
                "Настройки → Мои устройства → Удалить"));
        lines.add(infoLink("3. Либо обновите тариф",
                "Больше устройств → /start → Тарифы"));
        lines.add("");

        lines.add(paymentLink());

        return Base64.getEncoder().encodeToString(
                String.join("\n", lines).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Добавляет все ссылки одного устройства в список строк подписки.
     * Порядок: relay → direct → hy2
     */
    private void appendDeviceLinks(List<String> lines, VpnConfiguration deviceConfig) {
        if (deviceConfig.hasNoStoredLinks()) {
            log.warn("Config uuid={} has no stored links", deviceConfig.getVlessUuid());
            return;
        }

        String deviceSuffix = buildDeviceSuffix(deviceConfig);

        if (deviceConfig.getRelayLinks() != null) {
            deviceConfig.getRelayLinks().forEach(relay -> {
                String link = appendDescription(relay.getLink(), relay.getDescription(), deviceSuffix);
                lines.add(link);
            });
        }

        if (deviceConfig.getVlessLinks() != null) {
            deviceConfig.getVlessLinks().forEach(direct -> {
                String desc = formatDirectDescription(direct);
                String link = appendDescription(direct.getLink(), desc, deviceSuffix);
                lines.add(link);
            });
        }

        if (deviceConfig.getHy2Links() != null) {
            deviceConfig.getHy2Links().forEach(hy2 -> {
                lines.add(injectSuffix(hy2, deviceSuffix));
            });
        }

        lines.add("");
    }

    /**
     * Добавляет ?serverDescription= к ссылке.
     * Если ссылка уже содержит этот параметр — не дублирует.
     */
    private String appendDescription(String link, String description, String deviceSuffix) {
        if (link == null || link.isBlank()) return "";

        String finalLink = injectSuffix(link, deviceSuffix);

        if (description != null && !description.isBlank()
                && !finalLink.contains("serverDescription=")) {
            finalLink += "?serverDescription=" + b64(description);
        }
        return finalLink;
    }

    /**
     * Добавляет суффикс устройства к «имени» ссылки (часть после #).
     * Пример: vless://...#🇫🇮 Helsinki-1  →  vless://...#🇫🇮 Helsinki-1 (Устр. 2)
     */
    private String injectSuffix(String link, String suffix) {
        if (link == null || link.isBlank()) return "";
        if (suffix == null || suffix.isBlank()) return link;

        int hashIdx = link.indexOf('#');
        if (hashIdx == -1) {
            return link + "#" + encode(suffix.trim());
        }
        return link + encode(" " + suffix.trim());
    }

    /**
     * Суффикс типа " (Уст. 2)" если у пользователя больше одного устройства.
     * Для первого устройства — пустая строка.
     */
    private String buildDeviceSuffix(VpnConfiguration config) {
        List<VpnConfiguration> siblings = configRepository
                .findByUserIdAndStatus(config.getUserId(), ConfigStatus.ACTIVE);

        if (siblings.size() <= 1) return "";

        int idx = siblings.stream()
                .sorted(Comparator.comparing(VpnConfiguration::getCreatedAt))
                .map(VpnConfiguration::getId)
                .toList()
                .indexOf(config.getId()) + 1;

        String os = config.getDeviceOs() != null && !"Unknown".equals(config.getDeviceOs())
                ? " " + osEmoji(config.getDeviceOs())
                : "";

        return " (Уст. " + idx + os + ")";
    }

    private String formatDirectDescription(StoredVpnLinks.DirectLink direct) {
        StringBuilder sb = new StringBuilder("VLESS Direct");
        if (direct.getAvgLatencyMs() != null) {
            sb.append(" · ").append(direct.getAvgLatencyMs()).append("ms");
        }
        if (direct.getHealthScore() != null) {
            sb.append(" · ").append(String.format("%.0f%%", direct.getHealthScore())).append(" health");
        }
        return sb.toString();
    }

    private String buildLimitBanner() {
        return String.join("\n",
                "🚫 Лимит устройств исчерпан",
                "",
                "Чтобы продолжить использовать VPN:",
                "1. Откройте бот и удалите старые устройства",
                "2. Или обновите тариф для большего числа устройств",
                "",
                "📲 Перейдите в бот: @geovp_bot"
        );
    }

    private String infoLink(String name, String description) {
        return "vless://00000000-0000-0000-0000-000000000000@0.0.0.0:443"
                + "?encryption=none&security=none"
                + "&type=tcp"
                + "#" + encode(name)
                + "?serverDescription=" + b64(description);
    }

    private String paymentLink() {
        return "vless://00000000-0000-0000-0000-000000000001@1.1.1.1:443"
                + "?encryption=none&security=none"
                + "#🌐%20Личный%20кабинет%20/%20Оплата";
    }

    private String b64(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    private String encode(String text) {
        return java.net.URLEncoder.encode(text, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String osEmoji(String os) {
        if (os == null) return "";
        return switch (os.toLowerCase()) {
            case "ios", "macos" -> "🍎";
            case "android"      -> "🤖";
            case "windows"      -> "🪟";
            case "linux"        -> "🐧";
            default             -> "📱";
        };
    }
}