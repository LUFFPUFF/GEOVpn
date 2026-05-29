package com.vpn.config.service;

import com.vpn.common.dto.ServerDto;
import com.vpn.common.service.RedisCacheService;
import com.vpn.config.client.UserServiceClient;
import com.vpn.config.client.XUIServerApiClient;
import com.vpn.config.domain.entity.VpnBanLog;
import com.vpn.config.domain.entity.VpnConfiguration;
import com.vpn.common.dto.enums.ConfigStatus;
import com.vpn.config.repository.VpnBanLogRepository;
import com.vpn.config.repository.VpnConfigurationRepository;
import com.vpn.config.service.interf.ServerSelectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VpnSyncSchedulerService {

    private final VpnConfigurationRepository configRepository;
    private final ServerSelectionService     serverSelectionService;
    private final XUIServerApiClient         xuiClient;
    private final VpnLinksBuilder            vpnLinksBuilder;
    private final VpnBanLogRepository        banLogRepository;
    private final RedisCacheService          redisCacheService;
    private final UserServiceClient          userServiceClient;

    private final ExecutorService syncExecutor = Executors.newFixedThreadPool(10);

    /**
     * ПАРАЛЛЕЛЬНАЯ СИНХРОНИЗАЦИЯ (раз в час).
     *
     * ИСПРАВЛЕНИЕ: убрана аннотация @Transactional с этого метода.
     *
     * Проблема была в том, что @Transactional + CompletableFuture.allOf().join()
     * удерживали одну транзакцию (и одно соединение из пула) на всё время
     * параллельного выполнения — иногда десятки минут. Это приводило к истощению
     * пула соединений под нагрузкой. Каждый syncSingleConfig имеет собственную
     * @Transactional и управляет транзакцией независимо.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void synchronizeAllActiveConfigs() {
        log.info("STARTING HIGH-PERFORMANCE GLOBAL VPN SYNCHRONIZATION JOB...");

        List<ServerDto>       activeServers = serverSelectionService.getAllActiveServers();
        List<VpnConfiguration> activeConfigs = configRepository.findByStatus(ConfigStatus.ACTIVE);

        log.info("Found {} active servers and {} active configs. Processing in parallel...",
                activeServers.size(), activeConfigs.size());

        List<CompletableFuture<Void>> futures = activeConfigs.stream()
                .map(config -> CompletableFuture.runAsync(
                        () -> syncSingleConfig(config, activeServers), syncExecutor))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        log.info("HIGH-PERFORMANCE GLOBAL VPN SYNC COMPLETED.");
    }

    /**
     * Микро-транзакция для одного пользователя.
     * Транзакция открывается и закрывается строго в пределах этого метода,
     * не блокируя соединение на время сетевых вызовов к XUI.
     */
    @Transactional
    public void syncSingleConfig(VpnConfiguration config, List<ServerDto> activeServers) {
        try {
            vpnLinksBuilder.buildAndStore(config);
            configRepository.saveAndFlush(config);

            String email = buildEmail(config);
            String uuid  = config.getVlessUuid().toString();

            for (ServerDto server : activeServers) {
                List<Integer> targetInbounds = getTargetInbounds(server);
                if (!targetInbounds.isEmpty()) {
                    xuiClient.checkAndRestoreOrAttachClient(
                            server, targetInbounds, uuid, email, "xtls-rprx-vision");
                }
            }
        } catch (Exception e) {
            log.error("Failed to sync individual config ID {}: {}", config.getId(), e.getMessage());
        }
    }

    @Transactional
    public void banUserManually(Long userId, String reason) {
        log.info("Processing MANUAL BAN for userId={}, reason='{}'", userId, reason);

        List<VpnConfiguration> activeConfigs = configRepository.findByUserIdAndStatus(userId, ConfigStatus.ACTIVE);
        if (activeConfigs.isEmpty()) {
            log.warn("No active configs found for userId={} to ban", userId);
            return;
        }

        List<ServerDto> activeServers = serverSelectionService.getAllActiveServers();

        for (VpnConfiguration config : activeConfigs) {
            config.setStatus(ConfigStatus.BANNED);
            config.setRevokedAt(LocalDateTime.now());
            configRepository.save(config);

            for (ServerDto server : activeServers) {
                try {
                    xuiClient.removeClient(server, config.getVlessUuid().toString());
                } catch (Exception e) {
                    log.warn("Failed to remove manually banned client {} from server {}: {}",
                            config.getVlessUuid(), server.getName(), e.getMessage());
                }
            }

            VpnBanLog banLog = VpnBanLog.builder()
                    .userId(config.getUserId())
                    .deviceId(config.getDeviceId())
                    .bannedAt(LocalDateTime.now())
                    .reason(reason != null ? reason : "MANUAL_ADMIN_BAN")
                    .visitedDomains("MANUAL BAN - NO DOMAINS LOGGED")
                    .trafficConsumedMb(0L)
                    .status("ACTIVE")
                    .build();
            banLogRepository.save(banLog);

            redisCacheService.delete("vpn:meta:" + config.getVlessUuid());
        }

        log.info("User {} has been successfully banned manually.", userId);

        try {
            userServiceClient.updateBanStatus(userId, true, reason);
        } catch (Exception e) {
            log.error("Failed to sync manual ban status to user-service for user {}: {}", userId, e.getMessage());
        }
    }

    @Transactional
    public void unbanUser(Long userId) {
        log.info("Processing UNBAN for userId={}", userId);

        List<VpnConfiguration> bannedConfigs = configRepository.findByUserIdAndStatus(userId, ConfigStatus.BANNED);
        if (bannedConfigs.isEmpty()) {
            log.warn("No banned configs found for userId={}", userId);
            return;
        }

        banLogRepository.findByUserIdAndStatus(userId, "ACTIVE")
                .forEach(ban -> {
                    ban.setStatus("RESOLVED");
                    banLogRepository.save(ban);
                });

        List<ServerDto> activeServers = serverSelectionService.getAllActiveServers();

        for (VpnConfiguration config : bannedConfigs) {
            config.setStatus(ConfigStatus.ACTIVE);
            config.setRevokedAt(null);
            configRepository.save(config);

            vpnLinksBuilder.buildAndStore(config);
            configRepository.save(config);

            String email = buildEmail(config);
            String uuid  = config.getVlessUuid().toString();

            for (ServerDto server : activeServers) {
                List<Integer> targetInbounds = getTargetInbounds(server);
                if (!targetInbounds.isEmpty()) {
                    xuiClient.checkAndRestoreOrAttachClient(
                            server, targetInbounds, uuid, email, "xtls-rprx-vision");
                }
            }
        }

        log.info("User {} has been successfully unbanned and restored on all XUI panels.", userId);

        try {
            userServiceClient.updateBanStatus(userId, false, null);
        } catch (Exception e) {
            log.error("Failed to sync unban status to user-service for user {}: {}", userId, e.getMessage());
        }
    }

    private List<Integer> getTargetInbounds(ServerDto server) {
        List<Integer> inbounds = new ArrayList<>();
        if (server.getTcpInboundId() != null) inbounds.add(server.getTcpInboundId());
        if (server.getWsInboundId()  != null) inbounds.add(server.getWsInboundId());
        if (inbounds.isEmpty() && server.getPanelInboundId() != null) inbounds.add(server.getPanelInboundId());
        return inbounds;
    }

    private static String buildEmail(VpnConfiguration config) {
        return "tg_" + config.getUserId() + "_dev_" + config.getDeviceId();
    }
}