package com.vpn.config.service;

import com.vpn.common.dto.ServerDto;
import com.vpn.common.dto.response.DeviceResponse;
import com.vpn.common.dto.response.UserResponse;
import com.vpn.common.service.RedisCacheService;
import com.vpn.config.client.ServerManagementClient;
import com.vpn.config.client.UserServiceClient;
import com.vpn.config.client.XUIServerApiClient;
import com.vpn.config.domain.entity.VpnConfiguration;
import com.vpn.common.dto.enums.ConfigStatus;
import com.vpn.config.repository.VpnConfigurationRepository;
import com.vpn.config.service.interf.ServerSelectionService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class GlobalMaintenanceService {

    private static final int BATCH_SIZE = 200;

    private static final List<Integer> TOKEN_SERVER_IDS = List.of(8, 4, 3);
    private static final List<String> TOKEN_SERVER_TOKENS = List.of("UPDATE_ME_8", "UPDATE_ME_4", "UPDATE_ME_3");

    private final UserServiceClient userServiceClient;
    private final ServerManagementClient serverManagementClient;
    private final VpnConfigurationRepository configRepository;
    private final ServerSelectionService serverSelectionService;
    private final XUIServerApiClient xuiClient;
    private final VpnLinksBuilder vpnLinksBuilder;
    private final RedisCacheService redisCacheService;

    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /**
     * Регистрация нового эмиттера для админ-панели
     */
    public SseEmitter registerEmitter() {
        SseEmitter emitter = new SseEmitter(Duration.ofMinutes(15).toMillis());
        this.emitters.add(emitter);
        emitter.onCompletion(() -> this.emitters.remove(emitter));
        emitter.onTimeout(() -> this.emitters.remove(emitter));
        emitter.onError((e) -> this.emitters.remove(emitter));
        return emitter;
    }

    /**
     * Логирует сообщение в консоль бэкенда и одновременно транслирует его на фронтенд
     */
    private void broadcastLog(String message, String level) {
        switch (level) {
            case "INFO" -> log.info(message);
            case "WARN" -> log.warn(message);
            case "ERROR" -> log.error(message);
        }

        Map<String, String> logPayload = Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "level", level,
                "message", message
        );

        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("log").data(logPayload));
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        }
        emitters.removeAll(deadEmitters);
    }

    /**
     * Отправляет финальный отчет и закрывает соединения
     */
    private void broadcastReport(MaintenanceReport report) {
        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("report").data(report));
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        }
        emitters.removeAll(deadEmitters);
    }

    public MaintenanceReport runFullMaintenance() {
        long startMs = System.currentTimeMillis();
        broadcastLog("=== GLOBAL MAINTENANCE STARTED ===", "WARN");

        MaintenanceReport report = new MaintenanceReport();

        List<VpnConfiguration> activeConfigs = configRepository.findByStatus(ConfigStatus.ACTIVE);
        List<String> activeEmails = activeConfigs.stream()
                .map(config -> "tg_" + config.getUserId() + "_dev_" + config.getDeviceId())
                .distinct()
                .toList();
        broadcastLog("[STEP 2] Found " + activeEmails.size() + " active configurations in local DB to clear from XUI panels", "INFO");

        step3ClearXuiPanels(activeEmails, report);

        step4ClearDatabase();

        List<Long> targetUserIds = Collections.emptyList();
        try {
            var response = userServiceClient.getAllActiveUserIds();
            if (response != null && response.getData() != null) {
                targetUserIds = response.getData();
            }
        } catch (Exception e) {
            broadcastLog("[STEP 5] Failed to fetch active users from User-Service: " + e.getMessage(), "ERROR");
        }

        broadcastLog("[STEP 5] Found " + targetUserIds.size() + " active users from User-Service to re-sync", "INFO");

        step5BatchGenerateAndSync(targetUserIds, report);

        long elapsedSec = (System.currentTimeMillis() - startMs) / 1000;
        report.setElapsedSeconds(elapsedSec);

        broadcastLog("=== GLOBAL MAINTENANCE COMPLETED in " + elapsedSec + "s | processed=" + report.getUsersProcessed() + " | failed=" + report.getUsersFailed() + " ===", "WARN");

        broadcastReport(report);

        return report;
    }

    protected void step1UpdateServerTokens() {
        broadcastLog("[STEP 1] Updating API tokens for servers: " + TOKEN_SERVER_IDS, "INFO");
        for (int i = 0; i < TOKEN_SERVER_IDS.size(); i++) {
            Integer serverId = TOKEN_SERVER_IDS.get(i);
            String token = TOKEN_SERVER_TOKENS.get(i);
            try {
                Map<String, Object> updateRequest = Map.of("apiToken", token);
                serverManagementClient.updateServer(serverId, updateRequest);
                broadcastLog("Token updated via Feign for server id=" + serverId, "INFO");
            } catch (Exception e) {
                broadcastLog("Failed to update token via Feign for server id=" + serverId + ": " + e.getMessage(), "ERROR");
            }
        }
    }

    protected void step3ClearXuiPanels(List<String> activeEmails, MaintenanceReport report) {
        if (activeEmails.isEmpty()) {
            broadcastLog("[STEP 3] No active configurations found in local DB. Skipping XUI panels clear.", "WARN");
            return;
        }

        List<ServerDto> allServers = serverSelectionService.getAllActiveServers();
        broadcastLog("[STEP 3] Clearing " + activeEmails.size() + " targeted clients on " + allServers.size() + " servers in parallel...", "INFO");

        List<CompletableFuture<Void>> futures = allServers.stream()
                .map(server -> CompletableFuture.runAsync(() -> {
                    try {
                        broadcastLog("Removing " + activeEmails.size() + " targeted clients from server=" + server.getName() + "...", "INFO");
                        List<CompletableFuture<Void>> removeFutures = activeEmails.stream()
                                .map(email -> CompletableFuture.runAsync(() -> {
                                    try {
                                        xuiClient.deleteClientByEmail(server, email);
                                    } catch (Exception e) {
                                        broadcastLog("Failed to remove client " + email + " from " + server.getName() + ": " + e.getMessage(), "WARN");
                                    }
                                }, virtualExecutor))
                                .toList();

                        awaitAll(removeFutures);
                        report.incrementServersCleared();
                    } catch (Exception e) {
                        broadcastLog("Failed to clear panel for server=" + server.getName() + ": " + e.getMessage(), "ERROR");
                    }
                }, virtualExecutor))
                .toList();

        awaitAll(futures);
        broadcastLog("[STEP 3] Targeted XUI panels clearing completed.", "INFO");
    }

    protected void step4ClearDatabase() {
        broadcastLog("[STEP 4] Bulk-deleting all VPN configurations from DB...", "INFO");
        int deleted = configRepository.deleteAllConfigsFast();
        broadcastLog("Deleted " + deleted + " records from vpn_configurations.", "INFO");

        try {
            redisCacheService.deletePattern("vpn:meta:*");
            redisCacheService.deletePattern("vpn-configs*");
            broadcastLog("Redis VPN caches flushed.", "INFO");
        } catch (Exception e) {
            broadcastLog("Failed to flush Redis caches: " + e.getMessage(), "WARN");
        }
    }

    protected void step5BatchGenerateAndSync(List<Long> targetUserIds, MaintenanceReport report) {
        broadcastLog("[STEP 5] Starting batch generation & sync for " + targetUserIds.size() + " users...", "INFO");
        List<ServerDto> allServers = serverSelectionService.getAllActiveServers();

        AtomicInteger processed = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);

        for (int i = 0; i < targetUserIds.size(); i += BATCH_SIZE) {
            List<Long> batchIds = targetUserIds.subList(i, Math.min(i + BATCH_SIZE, targetUserIds.size()));
            broadcastLog("Processing batch " + ((i / BATCH_SIZE) + 1) + "/" + (int) Math.ceil((double) targetUserIds.size() / BATCH_SIZE), "INFO");
            processBatch(batchIds, allServers, processed, failed);
        }

        report.setUsersProcessed(processed.get());
        report.setUsersFailed(failed.get());
    }

    private void processBatch(List<Long> batchIds, List<ServerDto> allServers,
                              AtomicInteger processed, AtomicInteger failed) {

        List<VpnConfiguration> configsToSave = new ArrayList<>();
        Map<VpnConfiguration, Long> configToExpiryMap = new HashMap<>();

        for (Long userId : batchIds) {
            try {
                UserResponse user = userServiceClient.getUserByTelegramId(userId).getData();
                if (user == null || user.isBanned()) continue;

                LocalDateTime expiryDate = parseLocalDateTime(user.getSubscriptionExpiresAt());
                if (expiryDate == null || expiryDate.isBefore(LocalDateTime.now())) continue;

                long expiryTimeMillis = expiryDate.toInstant(ZoneOffset.UTC).toEpochMilli();

                List<DeviceResponse> devices = userServiceClient.getUserActiveDevices(userId).getData();
                if (devices == null || devices.isEmpty()) continue;

                for (DeviceResponse device : devices) {
                    VpnConfiguration config = buildNewConfig(user, device, allServers);
                    configsToSave.add(config);
                    configToExpiryMap.put(config, expiryTimeMillis);
                }

            } catch (Exception e) {
                broadcastLog("Failed to prepare config for userId=" + userId + ": " + e.getMessage(), "ERROR");
                failed.incrementAndGet();
            }
        }

        if (configsToSave.isEmpty()) return;

        List<VpnConfiguration> savedConfigs = configRepository.saveAll(configsToSave);

        List<CompletableFuture<Void>> xuiFutures = savedConfigs.stream()
                .map(config -> CompletableFuture.runAsync(() -> {
                    try {
                        long expiryMs = configToExpiryMap.getOrDefault(config, 0L);
                        syncConfigToXui(config, allServers, expiryMs);
                        processed.incrementAndGet();
                    } catch (Exception e) {
                        broadcastLog("XUI sync failed for uuid=" + config.getVlessUuid() + ": " + e.getMessage(), "ERROR");
                        failed.incrementAndGet();
                    }
                }, virtualExecutor))
                .toList();

        awaitAll(xuiFutures);
    }

    private VpnConfiguration buildNewConfig(UserResponse user, DeviceResponse device, List<ServerDto> allServers) {
        UUID vlessUuid = UUID.nameUUIDFromBytes((user.getTelegramId() + "_" + device.getId()).getBytes());

        ServerDto primaryServer = allServers.stream()
                .filter(s -> !s.isRelay())
                .findFirst()
                .orElse(allServers.getFirst());

        String sni = primaryServer.getRealitySni() != null ? primaryServer.getRealitySni() : "www.microsoft.com";
        String primaryLink = String.format("vless://%s@%s:%d?security=reality&encryption=none&pbk=%s&fp=chrome&sni=%s&sid=%s&type=tcp&flow=xtls-rprx-vision#GeoVPN",
                vlessUuid, primaryServer.getIpAddress(), primaryServer.getPort(),
                primaryServer.getRealityPublicKey(), sni, primaryServer.getRealityShortId());

        VpnConfiguration config = VpnConfiguration.builder()
                .vlessUuid(vlessUuid)
                .userId(user.getTelegramId())
                .deviceId(device.getId())
                .serverId(primaryServer.getId())
                .vlessLink(primaryLink)
                .deviceName(device.getDeviceName() != null ? device.getDeviceName() : "Unknown Device")
                .deviceOs(device.getDeviceType() != null ? device.getDeviceType().name() : "Unknown")
                .status(ConfigStatus.ACTIVE)
                .build();

        vpnLinksBuilder.buildAndStore(config);

        return config;
    }

    private void syncConfigToXui(VpnConfiguration config, List<ServerDto> allServers, long expiryTimeMillis) {
        String email = "tg_" + config.getUserId() + "_dev_" + config.getDeviceId();
        String uuid = config.getVlessUuid().toString();

        for (ServerDto server : allServers) {
            try {
                List<Integer> inboundIds = resolveInboundIds(server);
                if (inboundIds.isEmpty()) continue;

                xuiClient.addClientWithExpiryTime(server, inboundIds, uuid, email, "xtls-rprx-vision", expiryTimeMillis);
            } catch (Exception e) {
                broadcastLog("Failed to sync uuid=" + uuid + " to server=" + server.getName() + ": " + e.getMessage(), "WARN");
            }
        }
    }

    private List<Integer> resolveInboundIds(ServerDto server) {
        List<Integer> ids = new ArrayList<>();
        if (server.getTcpInboundId() != null) ids.add(server.getTcpInboundId());
        if (server.getWsInboundId() != null) ids.add(server.getWsInboundId());
        if (ids.isEmpty() && server.getPanelInboundId() != null) ids.add(server.getPanelInboundId());
        return ids;
    }

    private LocalDateTime parseLocalDateTime(Object obj) {
        return switch (obj) {
            case null -> null;
            case LocalDateTime ldt -> ldt;
            case String s -> {
                try {
                    yield LocalDateTime.parse(s);
                } catch (Exception e) {
                    broadcastLog("Failed to parse date: " + s, "WARN");
                    yield null;
                }
            }
            default -> null;
        };
    }

    private void awaitAll(List<CompletableFuture<Void>> futures) {
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    @Getter
    public static class MaintenanceReport {
        @Setter private int usersProcessed = 0;
        @Setter private int usersFailed = 0;
        private int serversCleared = 0;
        @Setter private long elapsedSeconds = 0;

        public void incrementServersCleared() { this.serversCleared++; }
    }
}