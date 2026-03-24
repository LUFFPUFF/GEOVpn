package com.vpn.server.scheduler;

import com.vpn.common.dto.ApiResponse;
import com.vpn.common.dto.ConfigMetadataDto;
import com.vpn.common.dto.request.ConnectionUpdateRequest;
import com.vpn.common.service.RedisCacheService;
import com.vpn.server.domain.entity.Server;
import com.vpn.server.domain.entity.TrafficUsage;
import com.vpn.server.grpc.UserServiceClient;
import com.vpn.server.grpc.XrayGrpcClient;
import com.vpn.server.repository.ServerRepository;
import com.vpn.server.repository.TrafficUsageRepository;
import com.vpn.server.service.TrafficCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrafficCollectorJob {

    private final ServerRepository serverRepository;
    private final XrayGrpcClient xrayGrpcClient;
    private final TrafficUsageRepository trafficRepository;
    private final TrafficCalculationService trafficCalculationService;
    private final RedisCacheService redisCacheService;
    private final UserServiceClient userClient;

    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    @Value("${vpn.billing.price-per-gb}")
    private double pricePerGb;

    @Scheduled(fixedDelayString = "${vpn.billing.collect-interval-ms:300000}")
    public void collectTraffic() {
        log.info("Starting global traffic collection cycle...");

        List<Server> activeServers = serverRepository.findByIsActiveTrue();
        if (activeServers.isEmpty()) {
            log.info("No active servers found.");
            return;
        }

        List<CompletableFuture<Void>> futures = activeServers.stream()
                .map(server -> CompletableFuture.runAsync(
                        () -> processServer(server), executor))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        log.info("Global traffic collection cycle finished.");
    }

    private void processServer(Server server) {
        try {
            var stats = xrayGrpcClient.getAllStatistics(
                    server.getIpAddress(), server.getGrpcPort());

            Map<String, List<com.vpn.server.grpc.generated.Stat>> groupedStats = stats.stream()
                    .filter(s -> s.getName().startsWith("user>>>"))
                    .collect(Collectors.groupingBy(
                            s -> s.getName().split(">>>")[1]));

            groupedStats.forEach((uuid, metrics) ->
                    handleUserStats(server, uuid, metrics));

        } catch (Exception e) {
            log.error("Critical error processing server {}: {}",
                    server.getName(), e.getMessage());
        }
    }

    private void handleUserStats(
            Server server,
            String userUuidStr,
            List<com.vpn.server.grpc.generated.Stat> metrics) {
        try {
            ConfigMetadataDto meta = redisCacheService.get(
                    "vpn:meta:" + userUuidStr, ConfigMetadataDto.class);

            if (meta == null) {
                log.debug("Metadata not found for UUID {}, skipping", userUuidStr);
                return;
            }

            long rawUp = 0, rawDown = 0;
            for (var m : metrics) {
                if (m.getName().contains("uplink"))   rawUp   = m.getValue();
                if (m.getName().contains("downlink")) rawDown = m.getValue();
            }

            long deltaUp   = trafficCalculationService.calculateDelta(
                    server.getId(), meta.getConfigId(), "up",   rawUp);
            long deltaDown = trafficCalculationService.calculateDelta(
                    server.getId(), meta.getConfigId(), "down", rawDown);
            long totalDelta = deltaUp + deltaDown;

            if (totalDelta <= 0) return;

            int cost = trafficCalculationService.calculateCost(totalDelta, pricePerGb);

            TrafficUsage usage = TrafficUsage.builder()
                    .userId(meta.getUserId())
                    .deviceId(meta.getDeviceId())
                    .serverId(server.getId())
                    .configId(meta.getConfigId())
                    .bytesIn(deltaUp)
                    .bytesOut(deltaDown)
                    .costKopecks(cost)
                    .build();
            trafficRepository.save(usage);

            updateConnection(meta, server.getId(), deltaUp, deltaDown);

            try {
                ApiResponse<Integer> billing =
                        userClient.deductBalance(meta.getUserId(), cost);

                if (billing.getData() != null && billing.getData() <= 0) {
                    log.warn("DISCONNECTING user {} — balance empty!", meta.getUserId());
                    xrayGrpcClient.removeUser(
                            server.getIpAddress(),
                            server.getGrpcPort(),
                            "vless-reality",
                            userUuidStr
                    );
                    closeConnection(meta, server.getId());
                }
            } catch (Exception e) {
                log.error("Billing failed for user {}: {}",
                        meta.getUserId(), e.getMessage());
            }

        } catch (Exception e) {
            log.error("Error handling stats for user {}: {}",
                    userUuidStr, e.getMessage());
        }
    }

    /**
     * Открывает новую сессию или обновляет байты в существующей.
     * Логика:
     * - Если открытой сессии нет — создаём новую
     * - Если есть — прибавляем дельту к bytesSent/bytesReceived
     */
    private void updateConnection(
            ConfigMetadataDto meta, Integer serverId,
            long deltaUp, long deltaDown) {

        ConnectionUpdateRequest request = ConnectionUpdateRequest.builder()
                .userId(meta.getUserId())
                .deviceId(meta.getDeviceId())
                .serverId(serverId)
                .deltaUp(deltaUp)
                .deltaDown(deltaDown)
                .build();

        try {
            userClient.updateConnection(request);
        } catch (Exception e) {
            log.error("Failed to update connection for userId={}: {}",
                    meta.getUserId(), e.getMessage());
        }
    }

    /**
     * Закрывает открытую сессию при отключении пользователя
     */
    private void closeConnection(ConfigMetadataDto meta, Integer serverId) {

        ConnectionUpdateRequest request = ConnectionUpdateRequest.builder()
                .userId(meta.getUserId())
                .deviceId(meta.getDeviceId())
                .serverId(serverId)
                .deltaUp(0L)
                .deltaDown(0L)
                .build();

        try {
            userClient.closeConnection(request);
        } catch (Exception e) {
            log.error("Failed to close connection for userId={}: {}",
                    meta.getUserId(), e.getMessage());
        }
    }
}
