package com.vpn.config.service;

import com.vpn.config.domain.entity.DeviceLimit;
import com.vpn.config.exception.DeviceLimitExceededException;
import com.vpn.config.repository.DeviceLimitRepository;
import com.vpn.config.repository.DeviceSessionRepository;
import com.vpn.config.repository.VpnConfigurationRepository;
import com.vpn.common.dto.enums.ConfigStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceLimitService {

    private final DeviceLimitRepository deviceLimitRepository;
    private final VpnConfigurationRepository configurationRepository;

    private static final int DEFAULT_MAX_DEVICES = 1;

    /**
     * Проверяет не превышен ли лимит устройств.
     * Вызывается перед созданием новой конфигурации.
     *
     * @throws DeviceLimitExceededException если лимит превышен
     */
    public void checkDeviceLimit(Long userId) {
        int maxDevices = getMaxDevices(userId);
        int activeDevices = countActiveDevices(userId);

        log.debug("Device limit check: userId={}, active={}, max={}",
                userId, activeDevices, maxDevices);

        if (activeDevices >= maxDevices) {
            log.warn("Device limit exceeded: userId={}, active={}, max={}",
                    userId, activeDevices, maxDevices);
            throw new DeviceLimitExceededException(userId, activeDevices, maxDevices);
        }
    }

    /**
     * Получить текущий лимит пользователя
     */
    public int getMaxDevices(Long telegramId) {
        return deviceLimitRepository.findByUserId(telegramId)
                .filter(DeviceLimit::isActive)
                .map(DeviceLimit::getMaxDevices)
                .orElse(1);
    }

    /**
     * Получить количество активных устройств
     */
    public int countActiveDevices(Long telegramId) {
        return configurationRepository
                .findByUserIdAndStatus(telegramId, ConfigStatus.ACTIVE)
                .size();
    }

    /**
     * Установить лимит пользователю (при покупке плана)
     */
    @Transactional
    public void setDeviceLimit(
            Long userId, int maxDevices, String planName, LocalDateTime expiresAt) {

        DeviceLimit limit = deviceLimitRepository.findByUserId(userId)
                .orElse(DeviceLimit.builder()
                        .userId(userId)
                        .build());

        limit.setMaxDevices(maxDevices);
        limit.setPlanName(planName);
        limit.setExpiresAt(expiresAt);

        deviceLimitRepository.save(limit);

        log.info("Device limit set: userId={}, max={}, plan={}, expires={}",
                userId, maxDevices, planName, expiresAt);
    }

    /**
     * Получить статус лимита для отображения пользователю
     */
    public DeviceLimitStatus getStatus(Long userId) {
        int max = getMaxDevices(userId);
        int active = countActiveDevices(userId);

        return DeviceLimitStatus.builder()
                .userId(userId)
                .maxDevices(max)
                .activeDevices(active)
                .remainingSlots(Math.max(0, max - active))
                .limitReached(active >= max)
                .build();
    }

    @Transactional
    public DeviceLimit ensureLimitInitialized(Long userId, String subscriptionType, LocalDateTime expiresAt) {
        return deviceLimitRepository.findByUserId(userId)
                .orElseGet(() -> {
                    int devices = switch (subscriptionType) {
                        case "BASIC" -> 1;
                        case "STANDARD" -> 3;
                        case "FAMILY" -> 5;
                        case "BUSINESS" -> 10;
                        case "UNLIMITED" -> 100;
                        default -> 1;
                    };

                    DeviceLimit newLimit = DeviceLimit.builder()
                            .userId(userId)
                            .maxDevices(devices)
                            .planName(subscriptionType)
                            .expiresAt(expiresAt)
                            .build();
                    return deviceLimitRepository.save(newLimit);
                });
    }

    public boolean isLimitExceeded(Long userId) {
        return countActiveDevices(userId) >= getMaxDevices(userId);
    }

    @lombok.Builder
    @lombok.Data
    public static class DeviceLimitStatus {
        private Long userId;
        private int maxDevices;
        private int activeDevices;
        private int remainingSlots;
        private boolean limitReached;
    }
}
