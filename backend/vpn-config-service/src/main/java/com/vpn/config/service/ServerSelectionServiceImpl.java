package com.vpn.config.service;

import com.vpn.config.client.ResilientServerManagementClient;
import com.vpn.config.config.VpnConfigProperties;
import com.vpn.common.dto.ServerDto;
import com.vpn.config.dto.request.ServerSelectionRequest;
import com.vpn.config.dto.response.ServerSelectionResult;
import com.vpn.config.exception.NoAvailableServerException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Реализация алгоритма выбора оптимального сервера
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServerSelectionServiceImpl implements com.vpn.config.service.interf.ServerSelectionService {

    private final ResilientServerManagementClient resilientServerClient;
    private final VpnConfigProperties configProperties;

    @Override
    public ServerSelectionResult selectBestServer(ServerSelectionRequest request) {
        log.info("Selecting best server for user: {}, location: {}",
                request.getUserId(), request.getUserLocation());

        List<ServerDto> activeServers = getAllActiveServers();

        if (activeServers.isEmpty()) {
            log.error("No active servers available");
            throw new NoAvailableServerException("No active servers available");
        }

        List<ServerDto> availableServers = activeServers.stream()
                .filter(ServerDto::isAvailable)
                .toList();

        if (availableServers.isEmpty()) {
            log.error("No available servers (all are full or inactive)");
            throw new NoAvailableServerException("All servers are currently full or inactive");
        }

        List<ServerSelectionResult> scoredServers = availableServers.stream()
                .map(server -> calculateServerScore(server, request.getUserLocation()))
                .sorted(Comparator.comparing(ServerSelectionResult::getTotalScore).reversed())
                .toList();

        ServerSelectionResult bestServer = scoredServers.getFirst();

        log.info("Selected server: {} ({}), score: {:.2f}, latency: {}ms, load: {:.1f}%",
                bestServer.getServer().getName(),
                bestServer.getServer().getLocation(),
                bestServer.getTotalScore(),
                bestServer.getServer().getAvgLatencyMs(),
                bestServer.getServer().getLoadPercentage());

        return bestServer;
    }

    @Override
    public List<ServerSelectionResult> getTopServers(ServerSelectionRequest request, int topN) {
        log.debug("Getting top {} servers for user: {}", topN, request.getUserId());

        List<ServerDto> activeServers = getAllActiveServers();

        return activeServers.stream()
                .filter(ServerDto::isAvailable)
                .map(server -> calculateServerScore(server, request.getUserLocation()))
                .sorted(Comparator.comparing(ServerSelectionResult::getTotalScore).reversed())
                .limit(topN)
                .collect(Collectors.toList());
    }

    @Override
    public ServerSelectionResult calculateServerScore(ServerDto server, String userLocation) {

        VpnConfigProperties.WeightsProperties weights = configProperties.getSelection().getWeights();

        double latencyScore = calculateLatencyScore(server.getAvgLatencyMs());

        double loadScore = calculateLoadScore(server.getLoadPercentage());

        double geographyScore = calculateGeographyScore(
                server.getCountryCode(),
                userLocation
        );

        double healthScore = calculateHealthScore(server.getHealthScore());

        double protocolScore = calculateProtocolScore(server);

        double totalScore =
                (latencyScore * weights.getLatency()) +
                        (loadScore * weights.getLoad()) +
                        (geographyScore * weights.getGeography()) +
                        (healthScore * weights.getHealth()) +
                        (protocolScore * weights.getProtocol());

        ServerSelectionResult.ScoreBreakdown breakdown = ServerSelectionResult.ScoreBreakdown.builder()
                .latencyScore(latencyScore)
                .loadScore(loadScore)
                .geographyScore(geographyScore)
                .healthScore(healthScore)
                .protocolScore(protocolScore)
                .build();

        String reason = buildSelectionReason(server, totalScore, breakdown);

        return ServerSelectionResult.builder()
                .server(server)
                .totalScore(totalScore)
                .scoreBreakdown(breakdown)
                .selectionReason(reason)
                .build();
    }

    @Override
    public List<ServerDto> getAllActiveServers() {
        log.debug("Fetching active servers from Server Management Service");

        try {
            List<ServerDto> servers = resilientServerClient.getActiveServers();
            log.info("Retrieved {} active servers", servers.size());
            return servers;
        } catch (Exception e) {
            log.error("Failed to fetch servers from Server Management Service", e);
            throw new NoAvailableServerException(
                    "Failed to fetch servers: " + e.getMessage());
        }
    }

    private double calculateLatencyScore(Integer latencyMs) {
        if (latencyMs == null || latencyMs <= 0) {
            return 50.0;
        }

        VpnConfigProperties.LatencyProperties latencyConfig =
                configProperties.getSelection().getLatency();

        double score = 100.0 - (latencyMs * latencyConfig.getMultiplier());

        if (latencyMs < latencyConfig.getExcellentThreshold()) {
            score += 5.0;
        } else if (latencyMs < latencyConfig.getGoodThreshold()) {
            score += 3.0;
        }

        if (latencyMs > latencyConfig.getMaxAcceptableMs()) {
            score *= 0.5;
        }

        return Math.max(0, Math.min(100, score));
    }

    private double calculateLoadScore(double loadPercentage) {
        VpnConfigProperties.LoadProperties loadConfig =
                configProperties.getSelection().getLoad();

        double score = 100.0 - loadPercentage;

        if (loadPercentage >= loadConfig.getCriticalThreshold()) {
            score *= 0.3;
        } else if (loadPercentage >= loadConfig.getWarningThreshold()) {
            score *= 0.7;
        } else if (loadPercentage < loadConfig.getOptimalThreshold()) {
            score += 5.0;
        }

        return Math.max(0, Math.min(100, score));
    }

    private double calculateGeographyScore(String serverCountry, String userLocation) {
        if (serverCountry == null) {
            return 0.0;
        }

        String userRegion = determineUserRegion(userLocation);

        int bonus = configProperties.getSelection()
                .getGeographyBonus(serverCountry, userRegion);

        if (userLocation != null && userLocation.equals(serverCountry)) {
            bonus += 10;
        }

        log.debug("Geography bonus for {}: {} points (user region: {})",
                serverCountry, bonus, userRegion);

        return Math.min(100, bonus);
    }

    private String determineUserRegion(String userLocation) {
        if (userLocation == null) {
            return "default";
        }

        Set<String> euCountries = Set.of("DE", "FR", "UK", "IT", "ES", "PT", "BE", "NL");
        if (euCountries.contains(userLocation)) {
            return "EU";
        }

        Set<String> asiaCountries = Set.of("CN", "JP", "KR", "SG", "HK", "TW", "IN");
        if (asiaCountries.contains(userLocation)) {
            return "ASIA";
        }

        return "default";
    }

    private double calculateHealthScore(Double healthScore) {
        if (healthScore == null) {
            return 50.0;
        }
        return Math.max(0, Math.min(100, healthScore));
    }

    private double calculateProtocolScore(ServerDto server) {
        boolean supportsReality = server.getRealityPublicKey() != null
                && !server.getRealityPublicKey().isEmpty();

        return supportsReality ? 10.0 : 5.0;
    }

    private String buildSelectionReason(
            ServerDto server,
            double totalScore,
            ServerSelectionResult.ScoreBreakdown breakdown) {

        VpnConfigProperties.WeightsProperties weights =
                configProperties.getSelection().getWeights();

        StringBuilder reason = new StringBuilder();
        reason.append(String.format("Server '%s' selected with score %.2f. ",
                server.getName(), totalScore));

        Map<String, Double> components = Map.of(
                "Low latency", breakdown.getLatencyScore() * weights.getLatency(),
                "Low load", breakdown.getLoadScore() * weights.getLoad(),
                "Good location", breakdown.getGeographyScore() * weights.getGeography(),
                "Excellent health", breakdown.getHealthScore() * weights.getHealth(),
                "Protocol support", breakdown.getProtocolScore() * weights.getProtocol()
        );

        String bestComponent = components.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Overall performance");

        reason.append("Key factor: ").append(bestComponent).append(".");

        return reason.toString();
    }
}
