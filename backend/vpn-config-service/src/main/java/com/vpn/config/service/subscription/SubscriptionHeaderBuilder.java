package com.vpn.config.service.subscription;

import com.vpn.config.domain.entity.DeviceLimit;
import com.vpn.config.repository.DeviceLimitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
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

    public HttpHeaders build(Long userId) {
        HttpHeaders headers = new HttpHeaders();
        Optional<DeviceLimit> limitOpt = deviceLimitRepository.findByUserId(userId);

        long expireTs = 0L;
        if (limitOpt.isPresent()) {
            DeviceLimit limit = limitOpt.get();
            if (limit.getExpiresAt() != null) {
                expireTs = limit.getExpiresAt().toEpochSecond(ZoneOffset.UTC);
            }
        }

        headers.set("subscription-userinfo",
                String.format("upload=0; download=0; total=0; expire=%d", expireTs));

        headers.set("profile-update-interval", "1");
        headers.set("sub-expire", "1");
        headers.set("content-disposition", "attachment; filename=geovpn.txt");

        return headers;
    }
}
