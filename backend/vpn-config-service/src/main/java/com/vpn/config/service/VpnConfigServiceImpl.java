package com.vpn.config.service;

import com.vpn.common.dto.ConfigMetadataDto;
import com.vpn.common.dto.ServerDto;
import com.vpn.common.dto.response.UserResponse;
import com.vpn.common.service.RedisCacheService;
import com.vpn.config.client.UserServiceClient;
import com.vpn.config.client.XUIServerApiClient;
import com.vpn.config.domain.entity.VpnConfiguration;
import com.vpn.common.dto.enums.ConfigStatus;
import com.vpn.common.dto.request.ConfigCreateRequest;
import com.vpn.common.dto.request.ConfigRegenerateRequest;
import com.vpn.common.dto.request.ServerSelectionRequest;
import com.vpn.common.dto.ServerSelectionResult;
import com.vpn.common.dto.response.VpnConfigResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Главный сервис управления VPN конфигурациями.
 *
 * Ключевое изменение архитектуры:
 * VLESS / relay / HY2 ссылки генерируются ОДИН РАЗ при createConfig/regenerateConfig
 * и сохраняются в БД (JSONB колонки vless_links_json, relay_links_json, hy2_link).
 * Подписка (SubscriptionService) только читает и раздаёт готовые ссылки.
 */
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

    @Value("${vpn.subscription.base-url:https://ways-parent-nokia-europe.trycloudflare.com}")
    private String subscriptionBaseUrl;

    @Override
    @Transactional
    public VpnConfigResponse createConfig(ConfigCreateRequest request) {
        log.info("Creating config: userId={}, deviceId={}", request.getUserId(), request.getDeviceId());

        Long tgId = request.getUserTelegramId();

        UserResponse user = userServiceClient.getUserByTelegramId(tgId).getData();
        if (user == null) throw new RuntimeException("User not found in user-service");

        deviceLimitService.ensureLimitInitialized(
                user.getTelegramId(),
                user.getSubscriptionType().name(),
                user.getSubscriptionExpiresAt()
        );

        boolean isNewDevice = configRepository
                .findByDeviceIdAndStatus(request.getDeviceId(), ConfigStatus.ACTIVE)
                .isEmpty();

        if (isNewDevice && deviceLimitService.isLimitExceeded(tgId)) {
            log.warn("Device limit exceeded for userId={}", tgId);
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
        syncWithXui(vlessUuid, allServers, user.getFirstName());

        ConfigMetadataDto meta = ConfigMetadataDto.builder()
                .configId(config.getId())
                .userId(config.getUserId())
                .deviceId(config.getDeviceId())
                .build();
        redisCacheService.set("vpn:meta:" + vlessUuid, meta, Duration.ofDays(30));

        List<VpnConfigResponse.ServerConfig> serverConfigs = buildResponseConfigs(config);

        log.info("Config created successfully: uuid={}, servers={}, relays={}, hy2={}",
                vlessUuid,
                config.getVlessLinks().size(),
                config.getRelayLinks().size(),
                config.getHy2Links() != null ? "yes" : "no");

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


    @Override
    public String getSubscription(UUID vlessUuid) {
        return subscriptionService.generateSubscription(vlessUuid);
    }

    @Override
    @Cacheable(value = "vpn-configs", key = "#deviceId")
    public VpnConfigResponse getConfigByDeviceId(Long deviceId) {
        VpnConfiguration config = configRepository
                .findByDeviceIdAndStatus(deviceId, ConfigStatus.ACTIVE)
                .orElseThrow(() -> new ConfigNotFoundException(deviceId));

        if (config.hasNoStoredLinks()) {
            rebuildLinks(config);
        }

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

    @Override
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
    public List<VpnConfigResponse> getActiveConfigs(Long userId) {
        return configRepository
                .findByUserIdAndStatus(userId, ConfigStatus.ACTIVE)
                .stream()
                .map(config -> getConfigByDeviceId(config.getDeviceId()))
                .collect(Collectors.toList());
    }


    @Override
    @Transactional
    @CachePut(value = "vpn-configs", key = "#deviceId")
    public VpnConfigResponse regenerateConfig(Long deviceId, ConfigRegenerateRequest request) {
        log.info("Regenerating config for device={}", deviceId);

        VpnConfiguration old = configRepository
                .findByDeviceIdAndStatus(deviceId, ConfigStatus.ACTIVE)
                .orElseThrow(() -> new ConfigNotFoundException(deviceId));

        old.revoke();
        configRepository.save(old);

        ConfigCreateRequest createRequest = ConfigCreateRequest.builder()
                .userId(old.getUserId())
                .userTelegramId(old.getUserId())
                .deviceId(deviceId)
                .preferredCountry(request.getPreferredCountry())
                .deviceOs(old.getDeviceOs())
                .deviceName(old.getDeviceName())
                .userLocation("RU")
                .build();

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

        allServers.forEach(server -> {
            try {
                xuiClient.removeClient(server, config.getVlessUuid().toString());
            } catch (Exception e) {
                log.warn("Failed to remove from XUI server={}: {}", server.getName(), e.getMessage());
            }
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

    /** Строит primary ссылку для QR-кода */
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

    /**
     * Формирует список ServerConfig для ответа клиенту.
     * Читает из уже сохранённых JSON-полей entity.
     * Порядок: relay → direct → hy2
     */
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
            config.getHy2Links().forEach(link -> {
                result.add(VpnConfigResponse.ServerConfig.builder()
                        .serverName("HY2 UDP Fallback")
                        .type("HY2")
                        .vlessLink(link)
                        .protocol("HY2")
                        .isRelay(false)
                        .build());
            });
        }

        return result;
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

    /**
     * Перестройка ссылок — используется для записей до миграции
     * или при ручном запросе обновления.
     */
    @Transactional
    public void rebuildLinks(VpnConfiguration config) {
        log.info("Rebuilding links for uuid={}", config.getVlessUuid());
        vpnLinksBuilder.buildAndStore(config);
        configRepository.save(config);
    }

    private void syncWithXui(UUID vlessUuid, List<ServerDto> servers, String email) {
        for (ServerDto server : servers) {
            try {

                String flow = server.isRelay() ? "xtls-rprx-vision" : "";

                xuiClient.addClient(server, vlessUuid.toString(), email, 0, flow);

                log.info("XUI sync success for server={}", server.getName());
            } catch (Exception e) {
                log.error("XUI sync FAIL: server={}, error={}", server.getName(), e.getMessage());
            }
        }
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
}