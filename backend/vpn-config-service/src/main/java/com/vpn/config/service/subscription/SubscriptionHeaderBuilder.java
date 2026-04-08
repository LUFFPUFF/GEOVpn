package com.vpn.config.service.subscription;

import com.vpn.config.domain.entity.DeviceLimit;
import com.vpn.config.repository.DeviceLimitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Optional;

/**
 * Строит HTTP-заголовки ответа подписки.
 *
 * subscription-userinfo — читается Happ/V2Box/Hiddify:
 *   upload=<bytes>; download=<bytes>; total=<bytes>; expire=<unix_ts>
 *
 * Лимит трафика рассчитывается по плану пользователя.
 */
@Component
@RequiredArgsConstructor
public class SubscriptionHeaderBuilder {

    private final DeviceLimitRepository deviceLimitRepository;
    private final SubscriptionBanner bannerBuilder;

    @Value("${vpn.support.telegram-url:https://t.me/geovpn_support}")
    private String supportTelegramUrl;

    @Value("${vpn.website-url:https://geovpn.com}")
    private String websiteUrl;

    @Value("${vpn.bot.upgrade-url:https://t.me/geovpn_bot?start=upgrade}")
    private String upgradeUrl;

    private static final java.util.Map<String, Long> PLAN_TRAFFIC_BYTES = java.util.Map.of(
            "BASIC",     100L  * 1024 * 1024 * 1024,
            "STANDARD",  300L  * 1024 * 1024 * 1024,
            "FAMILY",    500L  * 1024 * 1024 * 1024,
            "BUSINESS",  1024L * 1024 * 1024 * 1024,
            "UNLIMITED", 10L   * 1024 * 1024 * 1024 * 1024
    );

    public HttpHeaders build(Long userId) {
        HttpHeaders headers = new HttpHeaders();
        Optional<DeviceLimit> limitOpt = deviceLimitRepository.findByUserId(userId);

        long totalBytes = 100L * 1024 * 1024 * 1024;
        long expireTs   = 0L;

        if (limitOpt.isPresent()) {
            DeviceLimit limit = limitOpt.get();
            totalBytes = PLAN_TRAFFIC_BYTES.getOrDefault(limit.getPlanName(), totalBytes);
            if (limit.getExpiresAt() != null) {
                expireTs = limit.getExpiresAt().toEpochSecond(ZoneOffset.UTC);
            }
        }

        headers.set("profile-title",
                "base64:" + b64("GeoVPN"));

        headers.set("subscription-userinfo",
                String.format("upload=0; download=0; total=%d; expire=%d",
                        totalBytes, expireTs));

        headers.set("profile-update-interval", "1");

        headers.set("support-url", supportTelegramUrl);

        headers.set("profile-web-page-url", websiteUrl);

        headers.set("content-disposition",
                "attachment; filename=\"geovpn-" + userId + ".txt\"");

        String announcement = bannerBuilder.build(userId);
        headers.set("announce", "base64:" + b64(announcement));

        if (expireTs > 0) {
            headers.set("sub-expire", "1");
            headers.set("sub-expire-button-link", upgradeUrl);
            headers.set("notification-subs-expire", "1");
        }

        headers.set("hide-settings", "1");

        headers.set("subscription-ping-onopen-enabled", "1");

        headers.set("subscription-autoconnect", "1");
        headers.set("subscription-autoconnect-type", "lowestdelay");

        headers.set("fragmentation-enable", "1");
        headers.set("fragmentation-packets", "tlshello");
        headers.set("fragmentation-length", "100-200");
        headers.set("fragmentation-interval", "10-20");

        headers.set("ping-result", "icon");

        headers.set("subscription-auto-update-open-enable", "1");

        headers.set("subscriptions-collapse", "0");

        headers.set("sniffing-enable", "1");

        headers.set("ping-type", "proxy");
        headers.set("check-url-via-proxy",
                "https://cp.cloudflare.com/generate_204");

        return headers;
    }

    /**
     * Заголовки для блокирующей подписки (лимит превышен)
     */
    public HttpHeaders buildLimitExceeded(Long userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("profile-title", "base64:" + b64("⚠️ GeoVPN"));
        headers.set("profile-update-interval", "1");
        headers.set("profile-web-page-url", websiteUrl);

        headers.set("sub-info-color", "red");
        headers.set("sub-info-text",
                b64("🚫 Лимит устройств исчерпан! Удалите старое устройство или обновите тариф."));
        headers.set("sub-info-button-text", b64("Улучшить тариф"));
        headers.set("sub-info-button-link", upgradeUrl);
        headers.set("content-disposition",
                "attachment; filename=\"geovpn-blocked.txt\"");
        return headers;
    }

    private String b64(String text) {
        return Base64.getEncoder()
                .encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }
}
