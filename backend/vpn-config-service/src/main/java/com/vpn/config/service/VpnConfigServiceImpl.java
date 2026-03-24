package com.vpn.config.service;

import com.vpn.common.dto.ConfigMetadataDto;
import com.vpn.common.service.RedisCacheService;
import com.vpn.config.client.XUIServerApiClient;
import com.vpn.config.domain.entity.VpnConfiguration;
import com.vpn.common.dto.enums.ConfigStatus;
import com.vpn.config.domain.valueobject.ServerAddress;
import com.vpn.common.dto.ServerDto;
import com.vpn.config.dto.mapper.ConfigMapper;
import com.vpn.common.dto.request.ConfigCreateRequest;
import com.vpn.common.dto.request.ConfigRegenerateRequest;
import com.vpn.common.dto.request.ServerSelectionRequest;
import com.vpn.common.dto.ServerSelectionResult;
import com.vpn.common.dto.response.VpnConfigResponse;
import com.vpn.config.exception.ConfigNotFoundException;
import com.vpn.config.generator.*;
import com.vpn.config.generator.hysteria2.Hysteria2ConfigGenerator;
import com.vpn.config.repository.VpnConfigurationRepository;
import com.vpn.config.service.interf.ServerSelectionService;
import com.vpn.config.service.interf.VpnConfigService;
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
 * Главный сервис для управления VPN конфигурациями
 * Workflow:
 * 1. Выбрать оптимальный сервер (ServerSelectionService)
 * 2. Сгенерировать UUID и Reality параметры
 * 3. Построить VLESS ссылку
 * 4. Сгенерировать QR код
 * 5. Сохранить в БД
 * 6. Вернуть клиенту
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VpnConfigServiceImpl implements VpnConfigService {

    private final VpnConfigurationRepository configRepository;
    private final ServerSelectionService serverSelectionService;
    private final Hysteria2ConfigGenerator hysteria2ConfigGenerator;
    private final DeviceLimitService deviceLimitService;
    private final XUIServerApiClient xuiClient;
    private final SubscriptionBuilder subscriptionBuilder;
    private final SubscriptionService subscriptionService;
    private final UuidGenerator uuidGenerator;
    private final VlessLinkBuilder vlessLinkBuilder;
    private final QRCodeGenerator qrCodeGenerator;
    private final ConfigMapper configMapper;
    private final RedisCacheService redisCacheService;

    @Value("${vpn.subscription.base-url:https://api.geovpn.com}")
    private String subscriptionBaseUrl;

    @Value("${vpn.relay.ru.ip:}")
    private String ruRelayIp;

    @Override
    @Transactional
    public VpnConfigResponse createConfig(ConfigCreateRequest request) {
        log.info("Создание подписки для пользователя: {}, устройство: {}", request.getUserId(), request.getDeviceId());

        deviceLimitService.checkDeviceLimit(request.getUserId());

        UUID vlessUuid = uuidGenerator.generateDeterministicUuid(request.getUserId(), request.getDeviceId());

        List<ServerDto> allServers = serverSelectionService.getAllActiveServers();

        ServerSelectionRequest selectionRequest = ServerSelectionRequest.builder()
                .userId(request.getUserId())
                .userLocation(request.getUserLocation() != null ? request.getUserLocation() : "RU")
                .build();
        ServerSelectionResult serverResult = serverSelectionService.selectBestServer(selectionRequest);
        ServerDto primaryServer = serverResult.getServer();

        List<VpnConfigResponse.ServerConfig> allConfigs = buildAllServerConfigs(vlessUuid, allServers);

        String primaryLink = allConfigs.stream()
                .filter(c -> c.getServerId().equals(primaryServer.getId()) && !c.getIsRelay())
                .map(VpnConfigResponse.ServerConfig::getVlessLink)
                .findFirst()
                .orElse(allConfigs.getFirst().getVlessLink());

        VpnConfiguration config = VpnConfiguration.builder()
                .deviceId(request.getDeviceId())
                .userId(request.getUserId())
                .serverId(primaryServer.getId())
                .vlessUuid(vlessUuid)
                .vlessLink(primaryLink)
                .status(ConfigStatus.ACTIVE)
                .build();
        config = configRepository.save(config);

        String subscriptionBase64 = subscriptionService.generateSubscription(vlessUuid);
        String subscriptionUrl = subscriptionBaseUrl + "/api/v1/subscription/" + vlessUuid;

        String qrCodeDataUrl = qrCodeGenerator.generateQRCodeDataUrl(primaryLink);

        ConfigMetadataDto meta = ConfigMetadataDto.builder()
                .configId(config.getId())
                .userId(config.getUserId())
                .deviceId(config.getDeviceId())
                .build();
        redisCacheService.set("vpn:meta:" + vlessUuid, meta, Duration.ofDays(30));

        int limitIp = deviceLimitService.getMaxDevices(request.getUserId());
        syncWithXui(vlessUuid, allServers, limitIp);

        return VpnConfigResponse.builder()
                .id(config.getId())
                .deviceId(request.getDeviceId())
                .userId(request.getUserId())
                .subscriptionUrl(subscriptionUrl)
                .subscriptionBase64(subscriptionBase64)
                .configs(allConfigs)
                .qrCode(qrCodeDataUrl)
                .status(ConfigStatus.ACTIVE.name())
                .recommendedProtocol("VLESS")
                .selectionReason(serverResult.getSelectionReason())
                .serverScore(serverResult.getTotalScore())
                .availableProtocols(List.of("VLESS"))
                .build();
    }

    /**
     * Отдаёт base64 подписку по UUID пользователя.
     */
    @Override
    public String getSubscription(UUID vlessUuid) {
        return subscriptionService.generateSubscription(vlessUuid);
    }

    @Override
    @Cacheable(value = "vpn-configs", key = "#deviceId")
    public VpnConfigResponse getConfigByDeviceId(Long deviceId) {
        log.debug("Fetching config for device: {}", deviceId);

        VpnConfiguration config = configRepository
                .findByDeviceIdAndStatus(deviceId, ConfigStatus.ACTIVE)
                .orElseThrow(() -> new ConfigNotFoundException(deviceId));

        List<ServerDto> allServers = serverSelectionService.getAllActiveServers();
        List<VpnConfigResponse.ServerConfig> allConfigs =
                buildAllServerConfigs(config.getVlessUuid(), allServers);

        String subscriptionContent =
                subscriptionBuilder.buildSubscription(allConfigs);
        String subscriptionUrl = subscriptionBaseUrl
                + "/api/v1/subscription/" + config.getVlessUuid();

        String qrDataUrl = qrCodeGenerator
                .generateQRCodeDataUrl(config.getVlessLink());

        return VpnConfigResponse.builder()
                .id(config.getId())
                .deviceId(config.getDeviceId())
                .userId(config.getUserId())
                .configs(allConfigs)
                .subscriptionUrl(subscriptionUrl)
                .subscriptionBase64(subscriptionContent)
                .qrCode(qrDataUrl)
                .status(config.getStatus().name())
                .recommendedProtocol("VLESS")
                .availableProtocols(List.of("VLESS"))
                .build();
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
                xuiClient.removeClient(server.getId(), config.getVlessUuid().toString());
            } catch (Exception e) {
                log.warn("Failed to remove client from server {}: {}",
                        server.getId(), e.getMessage());
            }
        });

        config.revoke();
        configRepository.save(config);

        redisCacheService.delete("vpn:meta:" + config.getVlessUuid());
        log.info("Config revoked for device: {}", deviceId);
    }

    @Override
    @Transactional
    @CachePut(value = "vpn-configs", key = "#deviceId")
    public VpnConfigResponse regenerateConfig(
            Long deviceId, ConfigRegenerateRequest request) {
        log.info("Regenerating config for device: {}", deviceId);

        VpnConfiguration oldConfig = configRepository
                .findByDeviceIdAndStatus(deviceId, ConfigStatus.ACTIVE)
                .orElseThrow(() -> new ConfigNotFoundException(deviceId));

        oldConfig.revoke();
        configRepository.save(oldConfig);

        ConfigCreateRequest createRequest = ConfigCreateRequest.builder()
                .userId(oldConfig.getUserId())
                .deviceId(deviceId)
                .preferredCountry(request.getPreferredCountry())
                .userLocation("RU")
                .build();

        return createConfig(createRequest);
    }

    @Override
    public VpnConfigResponse getConfigByVlessUuid(UUID vlessUuid) {
        VpnConfiguration config = configRepository.findByVlessUuid(vlessUuid)
                .orElseThrow(() -> new ConfigNotFoundException(
                        "Config not found: " + vlessUuid));

        List<ServerDto> allServers = serverSelectionService.getAllActiveServers();
        List<VpnConfigResponse.ServerConfig> allConfigs =
                buildAllServerConfigs(vlessUuid, allServers);

        return VpnConfigResponse.builder()
                .id(config.getId())
                .configs(allConfigs)
                .subscriptionUrl(subscriptionBaseUrl
                        + "/api/v1/subscription/" + vlessUuid)
                .status(config.getStatus().name())
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
    public void updateLastUsed(UUID vlessUuid) {
        configRepository.findByVlessUuid(vlessUuid)
                .ifPresent(config -> {
                    config.updateLastUsed();
                    configRepository.save(config);
                });
    }

    @Override
    public boolean isConfigOwnedByUser(Long deviceId, Long userId) {
        return configRepository
                .findByDeviceIdAndStatus(deviceId, ConfigStatus.ACTIVE)
                .map(config -> config.getUserId().equals(userId))
                .orElse(false);
    }


    /**
     * Генерирует конфиги для ВСЕХ серверов.
     * Порядок: сначала RU relay/антиглушилки, потом EU серверы.
     */
    private List<VpnConfigResponse.ServerConfig> buildAllServerConfigs(UUID vlessUuid, List<ServerDto> servers) {
        List<VpnConfigResponse.ServerConfig> result = new ArrayList<>();
        int index = 1;

        for (ServerDto server : servers) {
            if (ruRelayIp != null && !ruRelayIp.isEmpty()) {
                String relayName = "🔘 Антиглушилка #" + server.getId() + " 4G/LTE 📶";
                result.add(VpnConfigResponse.ServerConfig.builder()
                        .serverId(server.getId())
                        .serverName(relayName)
                        .countryCode("RU")
                        .countryEmoji("🇷🇺")
                        .type("ANTIGLUSH")
                        .vlessLink(buildLinkForServer(vlessUuid, server, relayName, true))
                        .protocol("VLESS")
                        .isRelay(true)
                        .build());
            }

            String directName = getCountryEmoji(server.getCountryCode()) + " " + server.getName() + " | Direct 🚀";
            result.add(VpnConfigResponse.ServerConfig.builder()
                    .serverId(server.getId())
                    .serverName(directName)
                    .countryCode(server.getCountryCode())
                    .countryEmoji(getCountryEmoji(server.getCountryCode()))
                    .type("STANDARD")
                    .vlessLink(buildLinkForServer(vlessUuid, server, directName, false))
                    .protocol("VLESS")
                    .isRelay(false)
                    .avgLatencyMs(server.getAvgLatencyMs())
                    .build());
        }
        return result;
    }

    private String buildLinkForServer(
            UUID vlessUuid, ServerDto server, String name, boolean isRelay) {

        String address = isRelay ? ruRelayIp : server.getIpAddress();
        String sni = isRelay
                ? "eh.vk.com"
                : (server.getRealitySni() != null
                ? server.getRealitySni()
                : "eh.vk.com");

        return vlessLinkBuilder.buildVlessLinkCustom(
                vlessUuid,
                new ServerAddress(address),
                server.getPort(),
                name,
                server.getRealityPublicKey(),
                server.getRealityShortId(),
                sni,
                "chrome"
        );
    }

    private String getCountryEmoji(String code) {
        if (code == null) return "🌐";
        return switch (code.toUpperCase()) {
            case "NL" -> "🇳🇱";
            case "PL" -> "🇵🇱";
            case "FI" -> "🇫🇮";
            case "EE" -> "🇪🇪";
            case "RU" -> "🇷🇺";
            default -> "🌐";
        };
    }

    /**
     * Определяет тип сервера по его метаданным
     */
    private String detectServerType(ServerDto server) {
        if (server.getName() == null) return "STANDARD";
        String name = server.getName().toLowerCase();
        if (name.contains("antiglush") || name.contains("антиглуш")
                || name.contains("relay") || name.contains("lte")) {
            return "ANTIGLUSH";
        }
        if (name.contains("white") || name.contains("белый")
                || name.contains("whitelist")) {
            return "WHITELIST";
        }
        return "STANDARD";
    }

    private void syncWithXui(UUID vlessUuid, List<ServerDto> servers, int limitIp) {
        String email = vlessUuid.toString();

        servers.forEach(server -> {
            try {
                xuiClient.addClient(
                        XUIServerApiClient.PRIMARY_SERVER_ID,
                        vlessUuid.toString(),
                        email,
                        limitIp
                );
                log.info("Synced client to NL panel, server: {}", server.getName());
            } catch (Exception e) {
                log.error("Failed to sync with NL server {}: {}",
                        server.getName(), e.getMessage());
            }
        });

        if (ruRelayIp != null && !ruRelayIp.isEmpty()) {
            try {
                xuiClient.addClient(
                        XUIServerApiClient.RELAY_SERVER_ID,
                        vlessUuid.toString(),
                        email,
                        limitIp
                );
                log.info("Synced client to RU relay panel");
            } catch (Exception e) {
                log.error("Failed to sync with RU relay panel: {}", e.getMessage());
            }
        }
    }

}
