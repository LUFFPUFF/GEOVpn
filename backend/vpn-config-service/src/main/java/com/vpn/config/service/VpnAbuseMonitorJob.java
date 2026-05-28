package com.vpn.config.service;

import com.vpn.common.dto.ServerDto;
import com.vpn.common.dto.enums.ConfigStatus;
import com.vpn.common.service.RedisCacheService;
import com.vpn.config.client.UserServiceClient;
import com.vpn.config.client.XUIServerApiClient;
import com.vpn.config.domain.entity.VpnBanLog;
import com.vpn.config.domain.entity.VpnConfiguration;
import com.vpn.config.repository.VpnBanLogRepository;
import com.vpn.config.repository.VpnConfigurationRepository;
import com.vpn.config.service.interf.ServerSelectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VpnAbuseMonitorJob {

    private final VpnConfigurationRepository configRepository;
    private final ServerSelectionService     serverSelectionService;
    private final XUIServerApiClient         xuiClient;
    private final VpnBanLogRepository        banLogRepository;
    private final RedisCacheService          redisCacheService;
    private final UserServiceClient          userServiceClient;

    @Value("#{'${vpn.abuse.whitelist:}'.split(',')}")
    private List<Long> tgWhitelist;

    private static final long MAX_TRAFFIC_DELTA_BYTES = 3L * 1024 * 1024 * 1024;
    private static final long WEEKLY_MIN_SIGNIFICANT_TRAFFIC_BYTES = 100L * 1024 * 1024 * 1024;
    private static final double WEEKLY_ABUSE_MULTIPLIER = 5.0;

    private static final Pattern DOMAIN_PATTERN = Pattern.compile("(?i)(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z0-9][a-z0-9-]{0,61}[a-z0-9]");

    /**
     * Проверка злоупотреблений раз в 10 минут.
     * Количество HTTP-запросов снижено с (N пользователей * M серверов) до (M серверов).
     */
    @Scheduled(cron = "0 */10 * * * *")
    public void monitorAbuse() {
        log.info("STARTING OPTIMIZED 10-MINUTES VPN ABUSE MONITORING JOB...");

        List<ServerDto> relayServers = serverSelectionService.getAllActiveServers().stream()
                .filter(ServerDto::isRelay)
                .toList();

        Map<String, Map<String, Map<String, Long>>> bulkServerTraffic = new HashMap<>();
        for (ServerDto server : relayServers) {
            Map<String, Map<String, Long>> serverTraffic = xuiClient.getAllClientsTraffic(server);
            if (!serverTraffic.isEmpty()) {
                bulkServerTraffic.put(server.getName(), serverTraffic);
            }
        }

        List<VpnConfiguration> activeConfigs = configRepository.findByStatus(ConfigStatus.ACTIVE);

        for (VpnConfiguration config : activeConfigs) {
            if (tgWhitelist != null && tgWhitelist.contains(config.getUserId())) continue;

            String email = "tg_" + config.getUserId() + "_dev_" + config.getDeviceId();

            for (ServerDto server : relayServers) {
                Map<String, Map<String, Long>> serverData = bulkServerTraffic.get(server.getName());
                if (serverData == null || !serverData.containsKey(email)) continue;

                Map<String, Long> traffic = serverData.get(email);
                long currentTotal = traffic.get("up") + traffic.get("down");
                String redisKey = "vpn:traffic-cache:" + config.getVlessUuid();

                Long previousTotal = redisCacheService.get(redisKey, Long.class);
                redisCacheService.set(redisKey, currentTotal, Duration.ofHours(24));

                if (previousTotal != null) {
                    long delta = currentTotal - previousTotal;
                    if (delta > MAX_TRAFFIC_DELTA_BYTES) {
                        banUser(config, server, email, "TRAFFIC_SPIKE_ABUSE", currentTotal);
                        break;
                    }
                }

                String logs = xuiClient.getXrayLogs(server, 150, email);
                if (logs != null && !logs.isBlank()) {
                    Set<String> visitedDomains = extractDomains(logs);
                    if (containsTorrentDomains(visitedDomains)) {
                        banUser(config, server, email, "TORRENT_PROTOCOL_DETECTED", currentTotal);
                        break;
                    }
                }
            }
        }
        log.info("10-MINUTES VPN ABUSE MONITORING JOB FINISHED.");
    }

    /**
     * ОПТИМИЗИРОВАНО: Еженедельная чистка относительного перерасхода.
     */
    @Scheduled(cron = "0 0 3 * * SUN")
    public void runWeeklyAbuseCheck() {
        log.info("STARTING OPTIMIZED WEEKLY RELATIVE ABUSE CHECK...");

        List<ServerDto> relayServers = serverSelectionService.getAllActiveServers().stream()
                .filter(ServerDto::isRelay)
                .toList();

        Map<String, Map<String, Map<String, Long>>> bulkServerTraffic = new HashMap<>();
        for (ServerDto server : relayServers) {
            Map<String, Map<String, Long>> serverTraffic = xuiClient.getAllClientsTraffic(server);
            if (!serverTraffic.isEmpty()) {
                bulkServerTraffic.put(server.getName(), serverTraffic);
            }
        }

        List<VpnConfiguration> activeConfigs = configRepository.findByStatus(ConfigStatus.ACTIVE);
        Map<VpnConfiguration, Long> userConsumptions = new HashMap<>();

        long totalConsumption = 0;
        int activeUsersCount = 0;

        for (VpnConfiguration config : activeConfigs) {
            if (tgWhitelist != null && tgWhitelist.contains(config.getUserId())) continue;

            String email = "tg_" + config.getUserId() + "_dev_" + config.getDeviceId();
            long userTotalOnRelays = 0;

            for (ServerDto server : relayServers) {
                Map<String, Map<String, Long>> serverData = bulkServerTraffic.get(server.getName());
                if (serverData != null && serverData.containsKey(email)) {
                    Map<String, Long> stats = serverData.get(email);
                    userTotalOnRelays += (stats.get("up") + stats.get("down"));
                }
            }

            if (userTotalOnRelays > 0) {
                userConsumptions.put(config, userTotalOnRelays);
                totalConsumption += userTotalOnRelays;
                activeUsersCount++;
            }
        }

        if (activeUsersCount == 0) return;

        long averageConsumption = totalConsumption / activeUsersCount;
        long banThreshold = (long) (averageConsumption * WEEKLY_ABUSE_MULTIPLIER);
        if (banThreshold < WEEKLY_MIN_SIGNIFICANT_TRAFFIC_BYTES) {
            banThreshold = WEEKLY_MIN_SIGNIFICANT_TRAFFIC_BYTES;
        }

        log.info("Weekly Ban Threshold set to: {} MB", banThreshold / (1024 * 1024));

        for (Map.Entry<VpnConfiguration, Long> entry : userConsumptions.entrySet()) {
            VpnConfiguration config = entry.getKey();
            long totalUsed = entry.getValue();

            if (totalUsed > banThreshold) {
                String email = "tg_" + config.getUserId() + "_dev_" + config.getDeviceId();
                ServerDto server = relayServers.isEmpty() ? null : relayServers.get(0);
                banUser(config, server, email, "WEEKLY_RELATIVE_OVERCONSUMPTION", totalUsed);
            }
        }
    }

    @Transactional
    protected void banUser(VpnConfiguration config, ServerDto server, String email, String reason, long totalBytes) {
        log.warn("!!!! BANNING USER {} FOR {}. Total used: {} MB", email, reason, totalBytes / (1024 * 1024));

        config.setStatus(ConfigStatus.BANNED);
        config.setRevokedAt(LocalDateTime.now());
        configRepository.save(config);

        String domainsList = "";
        if (server != null) {
            String logs = xuiClient.getXrayLogs(server, 500, email);
            Set<String> domains = extractDomains(logs);
            domainsList = domains.stream().limit(50).collect(Collectors.joining(", "));
        }

        List<ServerDto> allServers = serverSelectionService.getAllActiveServers();
        for (ServerDto s : allServers) {
            try {
                xuiClient.removeClient(s, config.getVlessUuid().toString());
            } catch (Exception ignored) {}
        }

        VpnBanLog banLog = VpnBanLog.builder()
                .userId(config.getUserId())
                .deviceId(config.getDeviceId())
                .bannedAt(LocalDateTime.now())
                .reason(reason)
                .visitedDomains(domainsList)
                .trafficConsumedMb(totalBytes / (1024 * 1024))
                .status("ACTIVE")
                .build();

        banLogRepository.save(banLog);
        redisCacheService.delete("vpn:meta:" + config.getVlessUuid());

        try {
            userServiceClient.updateBanStatus(config.getUserId(), true, reason);
        } catch (Exception e) {
            log.error("Failed to sync ban status to user-service for user {}: {}", config.getUserId(), e.getMessage());
        }
    }

    public Map<String, Set<String>> getUserDomainsOnDemand(Long userId) {
        log.info("On-demand domain lookup requested for userId={}", userId);
        Map<String, Set<String>> resultMap = new HashMap<>();

        List<VpnConfiguration> configs = configRepository.findByUserIdAndStatus(userId, ConfigStatus.ACTIVE);
        if (configs.isEmpty()) {
            configs = configRepository.findByUserIdAndStatus(userId, ConfigStatus.BANNED);
        }

        if (configs.isEmpty()) {
            log.warn("No active or banned configurations found for userId={}", userId);
            return resultMap;
        }

        List<ServerDto> activeServers = serverSelectionService.getAllActiveServers();

        for (VpnConfiguration config : configs) {
            String email = "tg_" + config.getUserId() + "_dev_" + config.getDeviceId();

            for (ServerDto server : activeServers) {
                try {
                    String logs = xuiClient.getXrayLogs(server, 1000, email);

                    if (logs != null && !logs.isBlank()) {
                        Set<String> domains = extractDomains(logs);

                        if (!domains.isEmpty()) {
                            String serverKey = server.getName() + " (" + server.getIpAddress() + ")";

                            resultMap.computeIfAbsent(serverKey, k -> new HashSet<>()).addAll(domains);
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to fetch on-demand logs for {} on {}: {}", email, server.getName(), e.getMessage());
                }
            }
        }

        return resultMap;
    }

    private Set<String> extractDomains(String logs) {
        Set<String> domains = new HashSet<>();
        Matcher matcher = DOMAIN_PATTERN.matcher(logs);
        while (matcher.find()) {
            String domain = matcher.group().toLowerCase();
            if (!isWhiteNoiseDomain(domain)) {
                domains.add(domain);
            }
        }
        return domains;
    }

    private boolean isWhiteNoiseDomain(String d) {
        return d.contains("yastatic") || d.contains("cloudfront") || d.contains("cloudflare")
                || d.contains("geovp") || d.contains("apple.com") || d.contains("microsoft")
                || d.contains("googleapis") || d.contains("googleusercontent") || d.contains("windowsupdate");
    }

    private boolean containsTorrentDomains(Set<String> domains) {
        List<String> forbiddenKeywords = List.of("torrent", "peer", "tracker", "bitshare", "rutracker", "utorrent");
        for (String domain : domains) {
            for (String keyword : forbiddenKeywords) {
                if (domain.contains(keyword)) return true;
            }
        }
        return false;
    }
}