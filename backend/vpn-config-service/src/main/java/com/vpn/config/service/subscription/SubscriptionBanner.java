package com.vpn.config.service.subscription;

import com.vpn.config.domain.entity.DeviceLimit;
import com.vpn.config.repository.DeviceLimitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Формирует текстовое объявление, которое отображается
 * в клиентах Happ / Hiddify под названием профиля.
 *
 * Поддерживается через заголовок "profile-description"
 * или как первая строка подписки (комментарий).
 */
@Component
@RequiredArgsConstructor
public class SubscriptionBanner {

    private final DeviceLimitRepository deviceLimitRepository;

    public String build(Long userId) {
        Optional<DeviceLimit> limitOpt = deviceLimitRepository.findByUserId(userId);

        String plan     = limitOpt.map(DeviceLimit::getPlanName).orElse("BASIC");
        int    maxDev   = limitOpt.map(DeviceLimit::getMaxDevices).orElse(1);
        String expires  = limitOpt
                .flatMap(l -> Optional.ofNullable(l.getExpiresAt()))
                .map(d -> d.toLocalDate().toString())
                .orElse("∞");

        return String.join("\n",
                "         Благодарим за приобретение GeoVPN",
                "📋 Тариф: " + formatPlan(plan),
                "📱 Устройств: " + maxDev,
                "📶 Глушат мобильный → Авто LTE (вверху списка)",
                "🌍 WiFi / не глушат → обычные локации"
        );
    }

    private String formatPlan(String plan) {
        return switch (plan) {
            case "BASIC"     -> "Базовый · 1 устройство";
            case "STANDARD"  -> "Стандартный · 3 устройства";
            case "FAMILY"    -> "Семейный · 5 устройств";
            case "BUSINESS"  -> "Бизнес · 10 устройств";
            case "UNLIMITED" -> "Безлимит ∞";
            default          -> plan;
        };
    }
}
