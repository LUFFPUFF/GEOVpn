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

    private static final long   MAX_TRAFFIC_DELTA_BYTES              = 3L  * 1024 * 1024 * 1024;
    private static final long   WEEKLY_MIN_SIGNIFICANT_TRAFFIC_BYTES = 100L * 1024 * 1024 * 1024;
    private static final double WEEKLY_ABUSE_MULTIPLIER              = 5.0;
    private static final int    MAX_DOMAINS_PER_BAN_LOG              = 50;

    @Scheduled(cron = "0 */10 * * * *")
    public void monitorAbuse() {
        log.info("STARTING OPTIMIZED 10-MINUTES VPN ABUSE MONITORING JOB...");

        List<ServerDto> relayServers = serverSelectionService.getAllActiveServers().stream()
                .filter(ServerDto::isRelay)
                .toList();

        Map<String, Map<String, Map<String, Long>>> bulkServerTraffic = fetchBulkTraffic(relayServers);

        List<VpnConfiguration> activeConfigs = configRepository.findByStatus(ConfigStatus.ACTIVE);

        for (VpnConfiguration config : activeConfigs) {
            if (isWhitelisted(config.getUserId())) continue;

            String email    = buildEmail(config);
            String redisKey = "vpn:traffic-cache:" + config.getVlessUuid();

            for (ServerDto server : relayServers) {
                Map<String, Map<String, Long>> serverData = bulkServerTraffic.get(server.getName());
                if (serverData == null || !serverData.containsKey(email)) continue;

                Map<String, Long> traffic     = serverData.get(email);
                long              currentTotal = traffic.get("up") + traffic.get("down");

                Long previousTotal = redisCacheService.get(redisKey, Long.class);
                redisCacheService.set(redisKey, currentTotal, Duration.ofHours(24));

                if (previousTotal != null) {
                    long delta = currentTotal - previousTotal;
                    if (delta > MAX_TRAFFIC_DELTA_BYTES) {
                        log.warn("Traffic spike detected for {} on {}: delta={} MB", email, server.getName(), delta / (1024 * 1024));
                        Set<String> domains = fetchDomainsLazy(server, email, 500);
                        banUser(config, domains, "TRAFFIC_SPIKE_ABUSE", currentTotal);
                        break;
                    }
                }
                Set<String> domains = fetchDomainsLazy(server, email, 150);
                if (containsTorrentDomains(domains)) {
                    log.warn("Torrent detected for {} on {}", email, server.getName());
                    banUser(config, domains, "TORRENT_PROTOCOL_DETECTED", currentTotal);
                    break;
                }
            }
        }

        log.info("10-MINUTES VPN ABUSE MONITORING JOB FINISHED.");
    }

    @Scheduled(cron = "0 0 3 * * SUN")
    public void runWeeklyAbuseCheck() {
        log.info("STARTING OPTIMIZED WEEKLY RELATIVE ABUSE CHECK...");

        List<ServerDto> relayServers = serverSelectionService.getAllActiveServers().stream()
                .filter(ServerDto::isRelay)
                .toList();

        Map<String, Map<String, Map<String, Long>>> bulkServerTraffic = fetchBulkTraffic(relayServers);
        List<VpnConfiguration> activeConfigs = configRepository.findByStatus(ConfigStatus.ACTIVE);

        Map<VpnConfiguration, Long> userConsumptions = new HashMap<>();
        long totalConsumption = 0;
        int  activeUsersCount = 0;

        for (VpnConfiguration config : activeConfigs) {
            if (isWhitelisted(config.getUserId())) continue;

            String email           = buildEmail(config);
            long   userTotalOnRelays = 0;

            for (ServerDto server : relayServers) {
                Map<String, Map<String, Long>> serverData = bulkServerTraffic.get(server.getName());
                if (serverData != null && serverData.containsKey(email)) {
                    Map<String, Long> stats = serverData.get(email);
                    userTotalOnRelays += stats.get("up") + stats.get("down");
                }
            }

            if (userTotalOnRelays > 0) {
                userConsumptions.put(config, userTotalOnRelays);
                totalConsumption += userTotalOnRelays;
                activeUsersCount++;
            }
        }

        if (activeUsersCount == 0) {
            log.info("Weekly check: no active traffic found.");
            return;
        }

        long banThreshold = Math.max(
                (long) ((double) totalConsumption / activeUsersCount * WEEKLY_ABUSE_MULTIPLIER),
                WEEKLY_MIN_SIGNIFICANT_TRAFFIC_BYTES
        );
        log.info("Weekly ban threshold: {} MB (avg={} MB, multiplier={})",
                banThreshold / (1024 * 1024),
                (totalConsumption / activeUsersCount) / (1024 * 1024),
                WEEKLY_ABUSE_MULTIPLIER);

        for (Map.Entry<VpnConfiguration, Long> entry : userConsumptions.entrySet()) {
            VpnConfiguration config    = entry.getKey();
            long             totalUsed = entry.getValue();
            if (totalUsed > banThreshold) {
                String email  = buildEmail(config);
                ServerDto server  = relayServers.isEmpty() ? null : relayServers.getFirst();
                Set<String> domains = server != null ? fetchDomainsLazy(server, email, 500) : Collections.emptySet();
                banUser(config, domains, "WEEKLY_RELATIVE_OVERCONSUMPTION", totalUsed);
            }
        }

        log.info("WEEKLY RELATIVE ABUSE CHECK FINISHED.");
    }

    /**
     * Банит пользователя. Домены передаются снаружи — уже собранные
     * в точке обнаружения нарушения, чтобы избежать дублирующего запроса.
     */
    @Transactional
    protected void banUser(VpnConfiguration config, Set<String> domains, String reason, long totalBytes) {
        String email = buildEmail(config);
        log.warn("!!!! BANNING USER {} FOR {}. Total used: {} MB", email, reason, totalBytes / (1024 * 1024));

        config.setStatus(ConfigStatus.BANNED);
        config.setRevokedAt(LocalDateTime.now());
        configRepository.save(config);

        String domainsList = domains.stream()
                .limit(MAX_DOMAINS_PER_BAN_LOG)
                .sorted()
                .collect(Collectors.joining(", "));

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
                .visitedDomains(domainsList.isBlank() ? "NO DOMAINS CAPTURED" : domainsList)
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
        log.info("On-demand domain lookup for userId={}", userId);
        Map<String, Set<String>> resultMap = new HashMap<>();

        List<VpnConfiguration> configs = configRepository.findByUserIdAndStatus(userId, ConfigStatus.ACTIVE);
        if (configs.isEmpty()) configs = configRepository.findByUserIdAndStatus(userId, ConfigStatus.BANNED);
        if (configs.isEmpty()) {
            log.warn("No active/banned configs for userId={}", userId);
            return resultMap;
        }

        List<ServerDto> activeServers = serverSelectionService.getAllActiveServers();

        for (VpnConfiguration config : configs) {
            String email = buildEmail(config);
            for (ServerDto server : activeServers) {
                try {
                    Set<String> domains = fetchDomainsLazy(server, email, 1000);
                    if (!domains.isEmpty()) {
                        String key = server.getName() + " (" + server.getIpAddress() + ")";
                        resultMap.computeIfAbsent(key, k -> new HashSet<>()).addAll(domains);
                    }
                } catch (Exception e) {
                    log.error("Failed to fetch on-demand logs for {} on {}: {}", email, server.getName(), e.getMessage());
                }
            }
        }
        return resultMap;
    }

    private Map<String, Map<String, Map<String, Long>>> fetchBulkTraffic(List<ServerDto> servers) {
        Map<String, Map<String, Map<String, Long>>> result = new HashMap<>();
        for (ServerDto server : servers) {
            Map<String, Map<String, Long>> serverTraffic = xuiClient.getAllClientsTraffic(server);
            if (!serverTraffic.isEmpty()) result.put(server.getName(), serverTraffic);
        }
        return result;
    }

    /**
     * Запрашивает логи Xray и парсит домены за один вызов.
     * Вынесен отдельно, чтобы не дублировать пару (getXrayLogs + extractDomains).
     */
    private Set<String> fetchDomainsLazy(ServerDto server, String email, int logLines) {
        String logs = xuiClient.getXrayLogs(server, logLines, email);
        if (logs == null || logs.isBlank()) return Collections.emptySet();
        return xuiClient.extractDomainsFromLogs(logs);
    }

    private boolean containsTorrentDomains(Set<String> domains) {
        List<String> forbiddenKeywords = List.of("torrent", "tracker", "bitshare", "rutracker", "utorrent", "piratebay", "1337x");
        for (String domain : domains) {
            for (String keyword : forbiddenKeywords) {
                if (domain.contains(keyword)) return true;
            }
        }
        return false;
    }

    private boolean isWhitelisted(Long userId) {
        return tgWhitelist != null && tgWhitelist.contains(userId);
    }

    /** Стандартный формат email в XUI: tg_{userId}_dev_{deviceId} */
    private static String buildEmail(VpnConfiguration config) {
        return "tg_" + config.getUserId() + "_dev_" + config.getDeviceId();
    }
}