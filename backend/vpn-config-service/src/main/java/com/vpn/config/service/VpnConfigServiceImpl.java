package com.vpn.config.service;

import com.vpn.common.dto.ConfigMetadataDto;
import com.vpn.common.dto.ServerDto;
import com.vpn.common.dto.response.UserResponse;
import com.vpn.common.service.RedisCacheService;
import com.vpn.config.client.ServerManagementClient;
import com.vpn.config.client.UserServiceClient;
import com.vpn.config.client.XUIServerApiClient;
import com.vpn.config.domain.entity.VpnConfiguration;
import com.vpn.common.dto.enums.ConfigStatus;
import com.vpn.common.dto.request.ConfigCreateRequest;
import com.vpn.common.dto.request.ConfigRegenerateRequest;
import com.vpn.common.dto.request.ServerSelectionRequest;
import com.vpn.common.dto.ServerSelectionResult;
import com.vpn.common.dto.response.VpnConfigResponse;
import com.vpn.config.domain.valueobject.StoredVpnLinks;
import com.vpn.config.dto.admin.AdminConfigDetailResponse;
import com.vpn.config.dto.admin.AdminConfigUpdateRequest;
import com.vpn.config.exception.ConfigNotFoundException;
import com.vpn.config.generator.*;
import com.vpn.config.repository.VpnConfigurationRepository;
import com.vpn.config.service.interf.ServerSelectionService;
import com.vpn.config.service.interf.VpnConfigService;
import com.vpn.config.service.subscription.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VpnConfigServiceImpl implements VpnConfigService {

    private final VpnConfigurationRepository configRepository;
    private final ServerSelectionService     serverSelectionService;
    private final DeviceLimitService         deviceLimitService;
    private final XUIServerApiClient         xuiClient;
    private final SubscriptionService        subscriptionService;
    private final UuidGenerator              uuidGenerator;
    private final VlessLinkBuilder           vlessLinkBuilder;
    private final QRCodeGenerator            qrCodeGenerator;
    private final RedisCacheService          redisCacheService;
    private final UserServiceClient          userServiceClient;
    private final VpnLinksBuilder            vpnLinksBuilder;
    private final ServerManagementClient serverManagementClient;

    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    @Value("${vpn.subscription.base-url:https://geovp.ru}")
    private String subscriptionBaseUrl;

    @Override
    @Transactional
    @Retryable(
            retryFor = DataIntegrityViolationException.class,
            maxAttempts = 2,
            backoff = @Backoff(delay = 100)
    )
    public VpnConfigResponse createConfig(ConfigCreateRequest request) {
        log.info("Creating config: userId={}, deviceId={}", request.getUserId(), request.getDeviceId());

        Optional<VpnConfiguration> existingConfigOpt = configRepository
                .findByDeviceIdAndStatus(request.getDeviceId(), ConfigStatus.ACTIVE);

        if (existingConfigOpt.isPresent()) {
            log.info("Active config already exists for deviceId={}, returning existing config.", request.getDeviceId());
            VpnConfiguration existingConfig = existingConfigOpt.get();

            if (existingConfig.hasNoStoredLinks()) {
                rebuildLinks(existingConfig);
            }
            return toConfigResponse(existingConfig);
        }

        Long tgId = request.getUserTelegramId();

        UserResponse user = userServiceClient.getUserByTelegramId(tgId).getData();
        if (user == null) throw new RuntimeException("User not found in user-service");

        deviceLimitService.ensureLimitInitialized(
                user.getTelegramId(),
                user.getSubscriptionType().name(),
                user.getSubscriptionExpiresAt()
        );

        enforceDeviceLimit(tgId);

        boolean isNewDevice = configRepository
                .findByDeviceIdAndStatus(request.getDeviceId(), ConfigStatus.ACTIVE)
                .isEmpty();

        if (isNewDevice && deviceLimitService.isLimitExceeded(tgId)) {
            log.warn("Device limit exceeded for userId={}, creation blocked", tgId);
            return buildLimitExceededResponse(request.getUserId());
        }

        UUID vlessUuid = uuidGenerator.generateDeterministicUuid(tgId, request.getDeviceId());

        ServerSelectionRequest selectionRequest = ServerSelectionRequest.builder()
                .userId(request.getUserId())
                .userLocation(request.getUserLocation() != null ? request.getUserLocation() : "RU")
                .build();
        ServerSelectionResult serverResult = serverSelectionService.selectBestServer(selectionRequest);
        ServerDto primaryServer = serverResult.getServer();

        VpnConfiguration config = configRepository.findByVlessUuid(vlessUuid)
                .orElse(VpnConfiguration.builder()
                        .vlessUuid(vlessUuid)
                        .userId(tgId)
                        .deviceId(request.getDeviceId())
                        .build());

        config.setServerId(primaryServer.getId());
        config.setStatus(ConfigStatus.ACTIVE);

        if (request.getDeviceName() != null) config.setDeviceName(request.getDeviceName());
        if (request.getDeviceOs()   != null) config.setDeviceOs(request.getDeviceOs());

        String primaryLink = buildPrimaryLink(vlessUuid, primaryServer);
        config.setVlessLink(primaryLink);

        vpnLinksBuilder.buildAndStore(config);

        config = configRepository.save(config);

        String qrCodeDataUrl = qrCodeGenerator.generateQRCodeDataUrl(primaryLink);
        String subscriptionBase64 = subscriptionService.generateSubscription(vlessUuid);
        String subscriptionUrl = subscriptionBaseUrl + "/api/v1/configs/subscription/" + vlessUuid;

        List<ServerDto> allServers = serverSelectionService.getAllActiveServers();

        String xuiLabel = buildXuiLabel(user.getUsername(), tgId, request.getDeviceId());

        boolean isSynced = syncWithXui(vlessUuid, allServers, xuiLabel);
        if (!isSynced) {
            throw new RuntimeException("Не удалось синхронизировать конфигурацию ни с одним сервером XUI. Откат транзакции.");
        }

        ConfigMetadataDto meta = ConfigMetadataDto.builder()
                .configId(config.getId())
                .userId(config.getUserId())
                .deviceId(config.getDeviceId())
                .build();
        redisCacheService.set("vpn:meta:" + vlessUuid, meta, Duration.ofDays(30));

        List<VpnConfigResponse.ServerConfig> serverConfigs = buildResponseConfigs(config);

        log.info("Config created: uuid={}, xuiLabel='{}', servers={}, relays={}, hy2={}",
                vlessUuid, xuiLabel,
                config.getVlessLinks().size(),
                config.getRelayLinks().size(),
                config.getHy2Links() != null ? "yes" : "no");

        deviceLimitService.markAsExtraDeviceIfNecessary(tgId, config.getDeviceId());

        return VpnConfigResponse.builder()
                .id(config.getId())
                .deviceId(request.getDeviceId())
                .userId(request.getUserId())
                .subscriptionUrl(subscriptionUrl)
                .subscriptionBase64(subscriptionBase64)
                .configs(serverConfigs)
                .qrCode(qrCodeDataUrl)
                .status(ConfigStatus.ACTIVE.name())
                .recommendedProtocol("VLESS")
                .selectionReason(serverResult.getSelectionReason())
                .serverScore(serverResult.getTotalScore())
                .availableProtocols(buildAvailableProtocols(config))
                .build();
    }

    /**
     * Оптимизировано: Принудительное соблюдение лимита устройств теперь работает асинхронно по сети.
     * База данных обновляется мгновенно, а удаление клиента с 10+ XUI-серверов уходит в фоновые виртуальные потоки.
     */
    private void enforceDeviceLimit(Long userId) {
        int maxDevices = deviceLimitService.getMaxDevices(userId);
        List<VpnConfiguration> activeConfigs = configRepository
                .findByUserIdAndStatus(userId, ConfigStatus.ACTIVE);

        int excess = activeConfigs.size() - maxDevices;
        if (excess <= 0) return;

        log.warn("Enforcing device limit for userId={}: {} excess device(s) will be removed (max={})",
                userId, excess, maxDevices);

        List<VpnConfiguration> toRevoke = activeConfigs.stream()
                .sorted(Comparator.comparing(
                        VpnConfiguration::getCreatedAt,
                        Comparator.nullsFirst(Comparator.reverseOrder())
                ))
                .limit(excess)
                .toList();

        List<ServerDto> allServers = serverSelectionService.getAllActiveServers();

        for (VpnConfiguration config : toRevoke) {
            try {
                config.revoke();
                configRepository.save(config);
                redisCacheService.delete("vpn:meta:" + config.getVlessUuid());

                userServiceClient.deleteDeviceById(config.getDeviceId(), userId);

                executorService.submit(() -> {
                    log.info("Async XUI cleanup started for revoked uuid={}", config.getVlessUuid());
                    for (ServerDto server : allServers) {
                        try {
                            xuiClient.removeClient(server, config.getVlessUuid().toString());
                        } catch (Exception e) {
                            log.warn("Async cleanup: failed for server={}, uuid={}: {}",
                                    server.getName(), config.getVlessUuid(), e.getMessage());
                        }
                    }
                });

                log.info("Enforcement (local sync success): removed deviceId={}, uuid={} for userId={}",
                        config.getDeviceId(), config.getVlessUuid(), userId);

            } catch (Exception e) {
                log.error("Enforcement: failed for deviceId={}, userId={}: {}",
                        config.getDeviceId(), userId, e.getMessage());
            }
        }
    }

    /**
     * Оптимизировано: Синхронизация с XUI-серверами теперь идет ПАРАЛЛЕЛЬНО.
     * Время ожидания ответа теперь равно времени ответа ОДНОГО самого быстрого сервера,
     * а не сумме времен всех серверов. Тормозящие сервера больше не блокируют транзакцию!
     */
    private boolean syncWithXui(UUID vlessUuid, List<ServerDto> servers, String email) {
        List<CompletableFuture<Boolean>> futures = servers.stream()
                .map(server -> CompletableFuture.supplyAsync(() -> {
                    try {
                        List<Integer> targetInbounds = new ArrayList<>();
                        if (server.getTcpInboundId() != null) targetInbounds.add(server.getTcpInboundId());
                        if (server.getWsInboundId() != null) targetInbounds.add(server.getWsInboundId());

                        String flow = "xtls-rprx-vision";
                        if (!targetInbounds.isEmpty()) {
                            xuiClient.addClientWithToken(
                                    server,
                                    targetInbounds,
                                    vlessUuid.toString(),
                                    email,
                                    flow,
                                    server.getApiToken()
                            );
                            log.info("XUI Multi-Inbound sync success: server={}, inboundIds={}", server.getName(), targetInbounds);
                            return true;
                        } else if (server.getPanelInboundId() != null) {
                            xuiClient.addClientWithToken(
                                    server,
                                    Collections.singletonList(server.getPanelInboundId()),
                                    vlessUuid.toString(),
                                    email,
                                    flow,
                                    server.getApiToken()
                            );
                            log.info("XUI Legacy/Relay sync success: server={}, inboundId={}", server.getName(), server.getPanelInboundId());
                            return true;
                        }
                    } catch (Exception e) {
                        log.error("XUI sync FAIL: server={}, error={}", server.getName(), e.getMessage());
                    }
                    return false;
                }, executorService))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .reduce(false, (a, b) -> a || b);
    }

    private String buildXuiLabel(String username, Long userId, Long deviceId) {
        return "tg_" + userId + "_dev_" + deviceId;
    }

    @Override
    public String getSubscription(UUID vlessUuid) {
        return subscriptionService.generateSubscription(vlessUuid);
    }

    @Override
    @Transactional
    @Cacheable(value = "vpn-configs", key = "#deviceId")
    public VpnConfigResponse getConfigByDeviceId(Long deviceId) {
        VpnConfiguration config = configRepository
                .findByDeviceIdAndStatus(deviceId, ConfigStatus.ACTIVE)
                .orElseThrow(() -> new ConfigNotFoundException(deviceId));

        if (config.hasNoStoredLinks()) {
            rebuildLinks(config);
        }

        return toConfigResponse(config);
    }

    @Override
    @Transactional
    public VpnConfigResponse getConfigByVlessUuid(UUID vlessUuid) {
        VpnConfiguration config = configRepository.findByVlessUuid(vlessUuid)
                .orElseThrow(() -> new ConfigNotFoundException("Config not found: " + vlessUuid));

        if (config.hasNoStoredLinks()) {
            rebuildLinks(config);
        }

        return VpnConfigResponse.builder()
                .id(config.getId())
                .deviceId(config.getDeviceId())
                .userId(config.getUserId())
                .subscriptionUrl(subscriptionBaseUrl + "/api/v1/subscription/" + vlessUuid)
                .configs(buildResponseConfigs(config))
                .status(config.getStatus().name())
                .availableProtocols(buildAvailableProtocols(config))
                .build();
    }

    @Override
    @Transactional
    public List<VpnConfigResponse> getActiveConfigs(Long userId) {
        return configRepository
                .findByUserIdAndStatus(userId, ConfigStatus.ACTIVE)
                .stream()
                .map(config -> {
                    if (config.hasNoStoredLinks()) rebuildLinks(config);
                    return toConfigResponse(config);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @CachePut(value = "vpn-configs", key = "#deviceId")
    public VpnConfigResponse regenerateConfig(Long deviceId, ConfigRegenerateRequest request) {
        log.info("Regenerating config for deviceId={}", deviceId);

        VpnConfiguration oldConfig = configRepository
                .findByDeviceIdAndStatus(deviceId, ConfigStatus.ACTIVE)
                .orElseThrow(() -> new ConfigNotFoundException(deviceId));

        List<ServerDto> allServers = serverSelectionService.getAllActiveServers();

        executorService.submit(() -> {
            log.info("Async cleanup of old client before regeneration started for uuid={}", oldConfig.getVlessUuid());
            for (ServerDto server : allServers) {
                try {
                    xuiClient.removeClient(server, oldConfig.getVlessUuid().toString());
                } catch (Exception e) {
                    log.warn("Async regen cleanup failed for server={}: {}", server.getName(), e.getMessage());
                }
            }
        });

        oldConfig.revoke();
        configRepository.save(oldConfig);
        redisCacheService.delete("vpn:meta:" + oldConfig.getVlessUuid());

        ConfigCreateRequest createRequest = ConfigCreateRequest.builder()
                .userId(oldConfig.getUserId())
                .userTelegramId(oldConfig.getUserId())
                .deviceId(deviceId)
                .preferredCountry(request.getPreferredCountry())
                .deviceOs(oldConfig.getDeviceOs())
                .deviceName(oldConfig.getDeviceName())
                .userLocation("RU")
                .build();

        log.info("Old config revoked and scheduled for XUI cleanup. Creating new config for device={}", deviceId);

        return createConfig(createRequest);
    }

    @Override
    @Transactional
    @CacheEvict(value = "vpn-configs", key = "#deviceId")
    public void revokeConfig(Long deviceId, Long userId) {
        VpnConfiguration config = configRepository
                .findByDeviceIdAndStatus(deviceId, ConfigStatus.ACTIVE)
                .orElseThrow(() -> new ConfigNotFoundException(deviceId));

        List<ServerDto> allServers = serverSelectionService.getAllActiveServers();

        executorService.submit(() -> {
            log.info("Async revoke config started for uuid={}", config.getVlessUuid());
            allServers.forEach(server -> {
                try {
                    xuiClient.removeClient(server, config.getVlessUuid().toString());
                } catch (Exception e) {
                    log.warn("Failed to remove from XUI server={}: {}", server.getName(), e.getMessage());
                }
            });
        });

        config.revoke();
        configRepository.save(config);
        redisCacheService.delete("vpn:meta:" + config.getVlessUuid());
        log.info("Config revoked: deviceId={}", deviceId);
    }

    @Override
    @Transactional
    public void updateLastUsed(UUID vlessUuid) {
        configRepository.findByVlessUuid(vlessUuid).ifPresent(config -> {
            config.updateLastUsed();
            configRepository.save(config);
        });
    }

    @Override
    public boolean isConfigOwnedByUser(Long deviceId, Long userId) {
        return configRepository
                .findByDeviceIdAndStatus(deviceId, ConfigStatus.ACTIVE)
                .map(c -> c.getUserId().equals(userId))
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminConfigDetailResponse getAdminConfigDetails(Long deviceId) {
        VpnConfiguration config = configRepository
                .findByDeviceIdAndStatus(deviceId, ConfigStatus.ACTIVE)
                .orElseThrow(() -> new ConfigNotFoundException("Config not found for device " + deviceId));

        if (config.hasNoStoredLinks()) {
            rebuildLinks(config);
        }

        return buildAdminResponse(config);
    }

    @Override
    @Transactional
    public AdminConfigDetailResponse updateConfigAndServers(Long deviceId, AdminConfigUpdateRequest request) {
        VpnConfiguration config = configRepository
                .findByDeviceIdAndStatus(deviceId, ConfigStatus.ACTIVE)
                .orElseThrow(() -> new ConfigNotFoundException("Config not found for device " + deviceId));

        if (request.getVlessLinks() != null) {
            config.setVlessLinks(request.getVlessLinks());
            for (StoredVpnLinks.DirectLink linkObj : request.getVlessLinks()) {
                updateServerFromVlessUri(linkObj.getServerId(), linkObj.getLink(), false);
            }
        }

        if (request.getRelayLinks() != null) {
            config.setRelayLinks(request.getRelayLinks());
            for (StoredVpnLinks.RelayLink linkObj : request.getRelayLinks()) {
                updateServerFromVlessUri(linkObj.getServerId(), linkObj.getLink(), true);
            }
        }

        if (request.getHy2Links() != null) {
            config.setHy2Links(request.getHy2Links());
        }

        configRepository.save(config);

        return buildAdminResponse(config);
    }

    private String buildPrimaryLink(UUID uuid, ServerDto server) {
        try {
            return vlessLinkBuilder.buildVlessLinkCustom(
                    uuid,
                    new com.vpn.config.domain.valueobject.ServerAddress(server.getIpAddress()),
                    server.getPort(),
                    "GeoVPN | " + server.getName(),
                    server.getRealityPublicKey(),
                    server.getRealityShortId(),
                    server.getRealitySni(),
                    "chrome"
            );
        } catch (Exception e) {
            log.error("Failed to build primary link for server={}: {}", server.getName(), e.getMessage());
            return "";
        }
    }

    private List<VpnConfigResponse.ServerConfig> buildResponseConfigs(VpnConfiguration config) {
        List<VpnConfigResponse.ServerConfig> result = new ArrayList<>();

        if (config.getRelayLinks() != null) {
            config.getRelayLinks().stream()
                    .sorted(Comparator.comparingInt(r -> r.getRelayPriority() != null ? r.getRelayPriority() : 0))
                    .forEach(relay -> result.add(
                            VpnConfigResponse.ServerConfig.builder()
                                    .serverId(relay.getServerId())
                                    .serverName(relay.getServerName())
                                    .countryCode(relay.getCountryCode())
                                    .countryEmoji(countryEmoji(relay.getCountryCode()))
                                    .type("ANTIGLUSH")
                                    .vlessLink(relay.getLink())
                                    .protocol("VLESS")
                                    .isRelay(true)
                                    .build()
                    ));
        }

        if (config.getVlessLinks() != null) {
            config.getVlessLinks().forEach(direct -> result.add(
                    VpnConfigResponse.ServerConfig.builder()
                            .serverId(direct.getServerId())
                            .serverName(direct.getServerName())
                            .countryCode(direct.getCountryCode())
                            .countryEmoji(countryEmoji(direct.getCountryCode()))
                            .type("STANDARD")
                            .vlessLink(direct.getLink())
                            .protocol("VLESS")
                            .isRelay(false)
                            .avgLatencyMs(direct.getAvgLatencyMs())
                            .healthScore(direct.getHealthScore())
                            .build()
            ));
        }

        if (config.getHy2Links() != null) {
            config.getHy2Links().forEach(link -> result.add(
                    VpnConfigResponse.ServerConfig.builder()
                            .serverName("HY2 UDP Fallback")
                            .type("HY2")
                            .vlessLink(link)
                            .protocol("HY2")
                            .isRelay(false)
                            .build()
            ));
        }

        return result;
    }

    private VpnConfigResponse toConfigResponse(VpnConfiguration config) {
        return VpnConfigResponse.builder()
                .id(config.getId())
                .deviceId(config.getDeviceId())
                .userId(config.getUserId())
                .subscriptionUrl(subscriptionBaseUrl + "/api/v1/subscription/" + config.getVlessUuid())
                .configs(buildResponseConfigs(config))
                .status(config.getStatus().name())
                .recommendedProtocol("VLESS")
                .availableProtocols(buildAvailableProtocols(config))
                .build();
    }

    private List<String> buildAvailableProtocols(VpnConfiguration config) {
        List<String> protocols = new ArrayList<>();
        protocols.add("VLESS");
        if (config.getRelayLinks() != null && !config.getRelayLinks().isEmpty()) {
            protocols.add("VLESS_RELAY");
        }
        if (config.getHy2Links() != null && !config.getHy2Links().isEmpty()) {
            protocols.add("HY2");
        }
        return protocols;
    }

    private VpnConfigResponse buildLimitExceededResponse(Long userId) {
        return VpnConfigResponse.builder()
                .userId(userId)
                .status("LIMIT_EXCEEDED")
                .subscriptionBase64(subscriptionService.generateLimitExceededSubscription(userId))
                .selectionReason("Превышен лимит устройств. Удалите старые устройства или обновите тариф.")
                .configs(Collections.emptyList())
                .availableProtocols(Collections.emptyList())
                .build();
    }

    @Transactional
    public void rebuildLinks(VpnConfiguration config) {
        log.info("Rebuilding links for uuid={}", config.getVlessUuid());
        vpnLinksBuilder.buildAndStore(config);
        configRepository.save(config);
    }

    @jakarta.annotation.PreDestroy
    public void shutdownExecutor() {
        log.info("Shutting down VpnConfigServiceImpl virtual threads executor...");
        executorService.shutdown();
    }

    private String countryEmoji(String code) {
        if (code == null) return "🌐";
        return switch (code.toUpperCase()) {
            case "NL" -> "🇳🇱"; case "DE" -> "🇩🇪"; case "FI" -> "🇫🇮";
            case "PL" -> "🇵🇱"; case "EE" -> "🇪🇪"; case "SE" -> "🇸🇪";
            case "FR" -> "🇫🇷"; case "GB" -> "🇬🇧"; case "US" -> "🇺🇸";
            case "LV" -> "🇱🇻"; case "LT" -> "🇱🇹"; case "RU" -> "🇷🇺";
            default   -> "🌐";
        };
    }

    private void updateServerFromVlessUri(Integer serverId, String vlessUri, boolean isRelay) {
        if (serverId == null || vlessUri == null || vlessUri.isBlank()) return;
        if (!vlessUri.startsWith("vless://")) return;

        try {
            ServerDto server = serverManagementClient.getServerById(serverId).getData();
            if (server == null) return;

            URI uri = new URI(vlessUri);
            String host = uri.getHost();
            int port = uri.getPort();

            Map<String, String> queryParams = new HashMap<>();
            String query = uri.getRawQuery();
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=", 2);
                    if (pair.length == 2) {
                        queryParams.put(
                                URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                                URLDecoder.decode(pair[1], StandardCharsets.UTF_8)
                        );
                    }
                }
            }

            Map<String, Object> updatePayload = new HashMap<>();
            boolean isUpdated = false;

            if (host != null && !host.equals(server.getIpAddress())) {
                updatePayload.put("ipAddress", host);
                isUpdated = true;
            }
            if (port > 0 && port != server.getPort()) {
                updatePayload.put("port", port);
                isUpdated = true;
            }

            String sni = queryParams.get("sni");
            String pbk = queryParams.get("pbk");
            String sid = queryParams.get("sid");

            if (isRelay) {
                if (sni != null && !sni.equals(server.getRelaySni())) { updatePayload.put("relaySni", sni); isUpdated = true; }
                if (pbk != null && !pbk.equals(server.getRelayPublicKey())) { updatePayload.put("relayPublicKey", pbk); isUpdated = true; }
                if (sid != null && !sid.equals(server.getRelayShortId())) { updatePayload.put("relayShortId", sid); isUpdated = true; }
            } else {
                if (sni != null && !sni.equals(server.getRealitySni())) { updatePayload.put("realitySni", sni); isUpdated = true; }
                if (pbk != null && !pbk.equals(server.getRealityPublicKey())) { updatePayload.put("realityPublicKey", pbk); isUpdated = true; }
                if (sid != null && !sid.equals(server.getRealityShortId())) { updatePayload.put("realityShortId", sid); isUpdated = true; }
            }

            if (isUpdated) {
                serverManagementClient.updateServer(serverId, updatePayload);
                log.info("Admin updated Server ID: {} via config link parsing (Feign Call)", serverId);
            }

        } catch (Exception e) {
            log.error("Failed to parse VLESS URI and update server ID {}: {}", serverId, e.getMessage());
        }
    }

    private AdminConfigDetailResponse buildAdminResponse(VpnConfiguration config) {
        return AdminConfigDetailResponse.builder()
                .id(config.getId())
                .deviceId(config.getDeviceId())
                .userId(config.getUserId())
                .vlessUuid(config.getVlessUuid())
                .status(config.getStatus().name())
                .deviceOs(config.getDeviceOs())
                .deviceName(config.getDeviceName())
                .vlessLinks(config.getVlessLinks())
                .relayLinks(config.getRelayLinks())
                .hy2Links(config.getHy2Links())
                .build();
    }
}