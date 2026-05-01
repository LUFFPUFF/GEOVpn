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
    private String supportUrl;

    @Value("${vpn.website-url:https://t.me/geovpn_bot}")
    private String websiteUrl;

    @Value("${vpn.bot.upgrade-url:https://t.me/geovpn_bot?start=upgrade}")
    private String upgradeUrl;

    @Value("${vpn.provider-id:geo-vpn-default-id}")
    private String providerId;


    public HttpHeaders build(Long userId) {
        HttpHeaders headers = new HttpHeaders();
        Optional<DeviceLimit> limitOpt = deviceLimitRepository.findByUserId(userId);

        long totalBytes = 100L * 1024 * 1024 * 1024;
        long expireTs = 0;

        if (limitOpt.isPresent()) {
            DeviceLimit limit = limitOpt.get();
            if (limit.getExpiresAt() != null) {
                expireTs = limit.getExpiresAt().toEpochSecond(ZoneOffset.UTC);
            }
        }

        headers.set("profile-title", "base64:" + b64("Geo VPN | " + userId));
        headers.set("profile-update-interval", "1");
        headers.set("support-url", supportUrl);
        headers.set("profile-web-page-url", "https://t.me/geovpn_bot");
        headers.set("providerid", providerId);

        headers.set("subscription-userinfo",
                String.format("upload=0; download=0; total=%d; expire=%d", totalBytes, expireTs));

        headers.set("announce", "base64:" + b64(bannerBuilder.build(userId)));
        headers.set("ping-result", "icon");
        headers.set("subscriptions-collapse", "0");
        headers.set("hide-settings", "1");

        headers.set("subscription-autoconnect", "1");
        headers.set("subscription-autoconnect-type", "lowestdelay");
        headers.set("subscription-auto-update-open-enable", "1");

        if (expireTs > 0) {
            headers.set("sub-expire", "1");
            headers.set("notification-subs-expire", "1");
        }

        headers.set("sub-info-color", "green");
        headers.set("sub-info-text", b64("🛡️ Ваше соединение защищено Geo VPN"));
        headers.set("sub-info-button-text", b64("Продлить"));
        headers.set("sub-info-button-link", "https://t.me/geovpn_bot?start=renew");

        headers.set("fragmentation-enable", "1");
        headers.set("fragmentation-packets", "tlshello");
        headers.set("fragmentation-length", "100-200");
        headers.set("fragmentation-interval", "10");

        headers.set("content-disposition", "attachment; filename=\"geovpn.txt\"");

        return headers;
    }

    public HttpHeaders buildLimitExceeded(Long userId) {
        HttpHeaders headers = new HttpHeaders();

        headers.set("profile-title", "base64:" + b64("⚠️ Geo VPN | ЛИМИТ"));
        headers.set("profile-update-interval", "1");
        headers.set("profile-web-page-url", websiteUrl);

        headers.set("sub-info-color", "red");
        headers.set("sub-info-text",
                b64("🚫 Лимит устройств исчерпан! Удалите старые устройства в боте или улучшите тариф."));

        headers.set("sub-info-button-text", b64("🚀 Улучшить тариф"));
        headers.set("sub-info-button-link", upgradeUrl);

        headers.set("hide-settings", "1");

        headers.set("subscription-autoconnect", "0");

        headers.set("content-disposition",
                "attachment; filename=\"geovpn-blocked.txt\"");

        return headers;
    }

    private String b64(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }
}
