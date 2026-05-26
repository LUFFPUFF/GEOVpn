package com.vpn.config.service.subscription;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vpn.config.client.UserServiceClient;
import com.vpn.config.client.XUIServerApiClient;
import com.vpn.config.domain.entity.DeviceLimit;
import com.vpn.config.repository.DeviceLimitRepository;
import com.vpn.config.repository.VpnConfigurationRepository;
import com.vpn.config.service.DeviceLimitService;
import com.vpn.config.service.interf.ServerSelectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionCleanupTask {

    private final DeviceLimitRepository deviceLimitRepository;
    private final VpnConfigurationRepository configRepository;
    private final XUIServerApiClient xuiClient;
    private final UserServiceClient userServiceClient;
    private final ServerSelectionService serverSelectionService;
    private final DeviceLimitService deviceLimitService;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cleanupExpiredSlots() {
        LocalDateTime now = LocalDateTime.now();
        List<DeviceLimit> expiredLimits = deviceLimitRepository.findAllByExpiresAtBefore(now);

        for (DeviceLimit limit : expiredLimits) {
            try {

                String raw = limit.getExtraDeviceIds();
                if (raw == null || raw.isBlank()) continue;
                List<Long> extraIds = objectMapper.readValue(raw, new TypeReference<>() {});

                if (extraIds != null && !extraIds.isEmpty()) {
                    log.info("Subscription expired for user {}. Removing {} extra devices.", limit.getUserId(), extraIds.size());

                    for (Long devId : extraIds) {
                        configRepository.findByDeviceId(devId).ifPresent(config -> {
                            serverSelectionService.getAllActiveServers().forEach(server -> {
                                try {
                                    xuiClient.removeClient(server, config.getVlessUuid().toString());
                                } catch (Exception e) {
                                    log.warn("Failed to remove extra client from XUI: {}", e.getMessage());
                                }
                            });
                            configRepository.delete(config);
                        });
                        try {
                            userServiceClient.deleteDeviceById(devId, limit.getUserId());
                        } catch (Exception e) {
                            log.warn("Failed to remove device from user-service: {}", e.getMessage());
                        }
                    }
                    limit.setMaxDevices(deviceLimitService.getBaseLimitForPlan(limit.getPlanName()));
                    limit.setExtraDeviceIds("[]");
                    deviceLimitRepository.save(limit);
                }
            } catch (Exception e) {
                log.error("Cleanup failed for user {}", limit.getUserId(), e);
            }
        }
    }
}
