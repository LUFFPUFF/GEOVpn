package com.vpn.config.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vpn.common.dto.response.DeviceLimitStatus;
import com.vpn.config.domain.entity.DeviceLimit;
import com.vpn.config.repository.DeviceLimitRepository;
import com.vpn.config.repository.VpnConfigurationRepository;
import com.vpn.common.dto.enums.ConfigStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceLimitService {

    private final DeviceLimitRepository      deviceLimitRepository;
    private final VpnConfigurationRepository configurationRepository;
    private final ObjectMapper               objectMapper;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void markAsExtraDeviceIfNecessary(Long userId, Long deviceId) {
        deviceLimitRepository.findByUserId(userId).ifPresent(limit -> {
            int baseLimit          = getBaseLimitForPlan(limit.getPlanName());
            int currentActiveCount = configurationRepository
                    .findByUserIdAndStatus(userId, ConfigStatus.ACTIVE).size();

            if (currentActiveCount <= baseLimit) return;

            try {
                List<Long> ids = parseExtraDeviceIds(limit.getExtraDeviceIds());

                if (ids.contains(deviceId)) {
                    log.debug("Device {} already marked as EXTRA for user {}, skipping", deviceId, userId);
                    return;
                }

                ids.add(deviceId);
                limit.setExtraDeviceIds(objectMapper.writeValueAsString(ids));
                deviceLimitRepository.save(limit);
                log.info("Device {} marked as EXTRA for user {}", deviceId, userId);
            } catch (Exception e) {
                log.error("Failed to update extra_device_ids for userId={}, deviceId={}: {}",
                        userId, deviceId, e.getMessage(), e);
            }
        });
    }

    @Transactional
    public void addExtraSlot(Long userId) {
        DeviceLimit limit = deviceLimitRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Limit not found for userId=" + userId));
        limit.setMaxDevices(limit.getMaxDevices() + 1);
        deviceLimitRepository.save(limit);
        log.info("Extra slot added for userId={}, new max={}", userId, limit.getMaxDevices());
    }

    public int getMaxDevices(Long telegramId) {
        return deviceLimitRepository.findByUserId(telegramId)
                .filter(DeviceLimit::isActive)
                .map(DeviceLimit::getMaxDevices)
                .orElse(1);
    }

    public int countActiveDevices(Long telegramId) {
        return configurationRepository
                .findByUserIdAndStatus(telegramId, ConfigStatus.ACTIVE)
                .size();
    }

    @Transactional
    public void setDeviceLimit(Long userId, int maxDevices, String planName, LocalDateTime expiresAt) {
        DeviceLimit limit = deviceLimitRepository.findByUserId(userId)
                .orElse(DeviceLimit.builder()
                        .userId(userId)
                        .extraDeviceIds("[]")
                        .build());

        limit.setMaxDevices(maxDevices);
        limit.setPlanName(planName);
        limit.setExpiresAt(expiresAt);

        if (limit.getExtraDeviceIds() == null || limit.getExtraDeviceIds().isBlank()) {
            limit.setExtraDeviceIds("[]");
        }

        deviceLimitRepository.save(limit);
        log.info("Device limit set: userId={}, max={}, plan={}, expires={}",
                userId, maxDevices, planName, expiresAt);
    }

    public DeviceLimitStatus getStatus(Long userId) {
        int max    = getMaxDevices(userId);
        int active = countActiveDevices(userId);
        return DeviceLimitStatus.builder()
                .userId(userId)
                .maxDevices(max)
                .activeDevices(active)
                .remainingSlots(Math.max(0, max - active))
                .limitReached(active >= max)
                .build();
    }

    /**
     * Инициализирует лимит при первом обращении пользователя.
     * extraDeviceIds = "[]" гарантирован здесь изначально.
     */
    @Transactional
    public void ensureLimitInitialized(Long userId, String subscriptionType, LocalDateTime expiresAt) {
        deviceLimitRepository.findByUserId(userId)
                .orElseGet(() -> {
                    int devices = getBaseLimitForPlan(subscriptionType);
                    DeviceLimit newLimit = DeviceLimit.builder()
                            .userId(userId)
                            .maxDevices(devices)
                            .planName(subscriptionType)
                            .expiresAt(expiresAt)
                            .extraDeviceIds("[]")
                            .build();
                    return deviceLimitRepository.save(newLimit);
                });
    }

    public boolean isLimitExceeded(Long userId) {
        return countActiveDevices(userId) >= getMaxDevices(userId);
    }

    public int getBaseLimitForPlan(String plan) {
        if (plan == null) return 1;
        return switch (plan.toUpperCase()) {
            case "DAILY"    -> 1;
            case "BASIC"    -> 1;
            case "STANDARD" -> 2;
            case "FAMILY"   -> 3;
            default         -> 1;
        };
    }

    /**
     * Безопасно парсит JSON-список deviceId.
     * Возвращает пустой список если значение null, пустое или невалидный JSON.
     * Это исправляет NPE для записей, созданных через setDeviceLimit() до фикса.
     */
    private List<Long> parseExtraDeviceIds(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            List<Long> ids = objectMapper.readValue(json, new TypeReference<List<Long>>() {});
            return ids != null ? ids : new ArrayList<>();
        } catch (Exception e) {
            log.warn("Could not parse extra_device_ids JSON '{}', resetting to empty list: {}", json, e.getMessage());
            return new ArrayList<>();
        }
    }
}