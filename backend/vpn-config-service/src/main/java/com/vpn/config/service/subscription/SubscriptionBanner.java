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

        String planName = limitOpt.map(DeviceLimit::getPlanName).orElse("BASIC");
        String expires = limitOpt
                .flatMap(l -> Optional.ofNullable(l.getExpiresAt()))
                .map(d -> d.toLocalDate().toString())
                .orElse("Unlimited");

        return String.join("\n",
                "🚀 GeoVPN Premium — Активен",
                "💳 Тариф: " + formatPlan(planName),
                "📅 Истекает: " + expires,
                "✨ Поддержка 24/7 в Telegram",
                "🛡️ Защита данных включена"
        );
    }

    private String formatPlan(String plan) {
        return switch (plan) {
            case "BASIC"     -> "Base 1/1";
            case "STANDARD"  -> "Standard 3/3";
            case "FAMILY"    -> "Family 5/5";
            case "BUSINESS"  -> "Business 10/10";
            case "UNLIMITED" -> "Unlimited ∞";
            default          -> plan;
        };
    }
}
