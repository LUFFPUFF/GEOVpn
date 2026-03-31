package com.vpn.config.service.subscription;

import org.springframework.stereotype.Component;
import java.util.*;

/**
 * Генерирует AdBlock-правила в формате Xray routing rules.
 *
 * В подписке клиент (V2Box/Hiddify) поддерживает JSON-конфиг
 * через отдельный endpoint fullConfig.
 *
 * Этот класс предоставляет список доменов для блокировки,
 * которые добавляются в routing.rules с outboundTag = "block".
 */
@Component
public class AdBlockConfigProvider {

    private static final List<String> ADS_DOMAINS = List.of(
            "domain:doubleclick.net", "domain:googlesyndication.com", "domain:googleadservices.com",
            "domain:adservice.google.com", "domain:adservice.google.ru", "domain:pagead2.googlesyndication.com",
            "domain:an.yandex.ru", "domain:mc.yandex.ru", "domain:yabs.yandex.ru",
            "domain:advertising.yandex.ru", "domain:bs.yandex.ru", "domain:adfox.ru",
            "domain:ads.vk.com", "domain:vk.com:ads", "domain:ads.mail.ru", "domain:mail.ru:ads",
            "domain:adnxs.com", "domain:advertising.com", "domain:taboola.com", "domain:outbrain.com"
    );

    private static final List<String> ANALYTICS_DOMAINS = List.of(
            "domain:google-analytics.com", "domain:analytics.google.com", "domain:stats.g.doubleclick.net",
            "domain:metrika.yandex.ru", "domain:metrika.yandex.com", "domain:counter.yadro.ru",
            "domain:top.mail.ru", "domain:amplitude.com", "domain:mixpanel.com", "domain:hotjar.com",
            "domain:app-measurement.com", "domain:firebaseinstallations.googleapis.com"
    );

    private static final List<String> TELEMETRY_DOMAINS = List.of(
            "domain:telemetry.microsoft.com", "domain:vortex.data.microsoft.com",
            "domain:tracking.miui.com", "domain:api.ad.xiaomi.com", "domain:data.mistat.xiaomi.com",
            "domain:metrics.icloud.com", "domain:metrics.apple.com"
    );

    /**
     * Возвращает плоский список всех доменов для блокировки
     */
    public List<String> getAllBlockedDomains() {
        List<String> all = new ArrayList<>();
        all.addAll(ADS_DOMAINS);
        all.addAll(ANALYTICS_DOMAINS);
        all.addAll(TELEMETRY_DOMAINS);
        return all;
    }

    /**
     * Генерирует массив правил маршрутизации (Routing Rules) для Xray/V2Ray JSON.
     * Эти правила говорят клиенту: "Если домен в списке — отправляй в outbound 'block'".
     */
    public List<Map<String, Object>> buildRoutingRules() {
        return List.of(
                Map.of(
                        "type", "field",
                        "domain", getAllBlockedDomains(),
                        "outboundTag", "block"
                )
        );
    }

    /**
     * Создает Outbound (выходной узел) типа "blackhole".
     * Весь трафик, попавший сюда, будет мгновенно сброшен (заблокирован).
     */
    public Map<String, Object> buildBlackHoleOutbound() {
        return Map.of(
                "tag", "block",
                "protocol", "blackhole",
                "settings", Map.of("response", Map.of("type", "none"))
        );
    }
}
