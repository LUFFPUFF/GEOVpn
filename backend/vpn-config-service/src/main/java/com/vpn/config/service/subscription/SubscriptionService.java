package com.vpn.config.service.subscription;

import com.vpn.common.dto.ServerDto;
import com.vpn.common.dto.enums.ConfigStatus;
import com.vpn.config.domain.entity.VpnConfiguration;
import com.vpn.config.exception.ConfigNotFoundException;
import com.vpn.config.repository.VpnConfigurationRepository;
import com.vpn.config.service.interf.ServerSelectionService;
import com.vpn.config.service.subscription.SubscriptionBanner;
import com.vpn.config.service.subscription.SubscriptionLinkFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Оркестрирует генерацию подписки.
 * Логика разнесена по фабрикам:
 *   — SubscriptionLinkFactory   → ссылки VLESS / Hysteria2
 *   — SubscriptionBanner        → текст объявления
 *   — SubscriptionHeaderBuilder → HTTP-заголовки (вызывается из контроллера)
 */
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final VpnConfigurationRepository configRepository;
    private final ServerSelectionService serverSelectionService;
    private final SubscriptionLinkFactory linkFactory;
    private final SubscriptionBanner banner;

    public String generateSubscription(UUID vlessUuid) {
        VpnConfiguration currentConfig = configRepository.findByVlessUuid(vlessUuid)
                .orElseThrow(() -> new RuntimeException("Not found"));

        List<VpnConfiguration> userConfigs = configRepository
                .findByUserIdAndStatus(currentConfig.getUserId(), ConfigStatus.ACTIVE);

        List<ServerDto> servers = serverSelectionService.getAllActiveServers();
        List<String> lines = new ArrayList<>();

        lines.add("#profile-title: GeoVPN");
        lines.add("#profile-update-interval: 1");

        int deviceIdx = 1;
        for (VpnConfiguration deviceConfig : userConfigs) {
            for (ServerDto server : servers) {
                lines.addAll(linkFactory.buildLinksForServer(deviceConfig.getVlessUuid(), server, deviceIdx));
            }
            deviceIdx++;
        }

        String raw = String.join("\n", lines);
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public String generateLimitExceededSubscription(Long userId) {
        List<String> lines = new ArrayList<>();

        lines.add("#profile-title: ⚠️ ЛИМИТ ПРЕВЫШЕН");
        lines.add("#announce: base64:" + Base64.getEncoder().encodeToString(
                "Вы достигли лимита устройств для вашего тарифа.\nПожалуйста, удалите старые устройства или обновите тариф в боте.".getBytes(StandardCharsets.UTF_8)));

        lines.add("#sub-info-color: red");
        lines.add("#sub-info-text: Лимит устройств исчерпан! Нажмите 'Продлить' для апгрейда.");
        lines.add("#sub-info-button-text: Улучшить тариф");
        lines.add("#sub-info-button-link: https://t.me/your_vpn_bot?start=upgrade");

        String descStep1 = Base64.getEncoder().encodeToString("Перейдите в настройки бота".getBytes());
        String descStep2 = Base64.getEncoder().encodeToString("Выберите пункт 'Мои устройства'".getBytes());
        String descStep3 = Base64.getEncoder().encodeToString("Удалите неиспользуемые".getBytes());

        lines.add("vless://limit-reached@0.0.0.0:443?encryption=none&security=reality#1. Как исправить??serverDescription=" + descStep1);
        lines.add("vless://limit-reached@0.0.0.0:443?encryption=none&security=reality#2. Удалите старые устройства?serverDescription=" + descStep2);
        lines.add("vless://limit-reached@0.0.0.0:443?encryption=none&security=reality#3. Либо купите подписку выше?serverDescription=" + descStep3);

        return Base64.getEncoder().encodeToString(String.join("\n", lines).getBytes(StandardCharsets.UTF_8));
    }
}
