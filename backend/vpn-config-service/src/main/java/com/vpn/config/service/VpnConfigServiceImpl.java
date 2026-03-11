package com.vpn.config.service;

import com.vpn.common.dto.ConfigMetadataDto;
import com.vpn.common.service.RedisCacheService;
import com.vpn.config.domain.entity.VpnConfiguration;
import com.vpn.common.dto.enums.ConfigStatus;
import com.vpn.common.dto.enums.ProtocolType;
import com.vpn.config.domain.valueobject.ServerAddress;
import com.vpn.common.dto.ServerDto;
import com.vpn.config.dto.mapper.ConfigMapper;
import com.vpn.common.dto.request.ConfigCreateRequest;
import com.vpn.common.dto.request.ConfigRegenerateRequest;
import com.vpn.common.dto.request.ServerSelectionRequest;
import com.vpn.common.dto.ServerSelectionResult;
import com.vpn.common.dto.response.VpnConfigResponse;
import com.vpn.config.exception.ConfigAlreadyExistsException;
import com.vpn.config.exception.ConfigNotFoundException;
import com.vpn.config.exception.UnauthorizedConfigAccessException;
import com.vpn.config.generator.QRCodeGenerator;
import com.vpn.config.generator.RealityConfigGenerator;
import com.vpn.config.generator.UuidGenerator;
import com.vpn.config.generator.VlessLinkBuilder;
import com.vpn.config.repository.VpnConfigurationRepository;
import com.vpn.config.service.interf.ServerSelectionService;
import com.vpn.config.service.interf.VpnConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
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
    private final UuidGenerator uuidGenerator;
    private final RealityConfigGenerator realityGenerator;
    private final VlessLinkBuilder vlessLinkBuilder;
    private final QRCodeGenerator qrCodeGenerator;
    private final ConfigMapper configMapper;
    private final RedisCacheService redisCacheService;

    @Override
    @Transactional
    public VpnConfigResponse createConfig(ConfigCreateRequest request) {
        log.info("Creating VPN config for user: {}, device: {}",
                request.getUserId(), request.getDeviceId());

        if (configRepository.existsByDeviceIdAndStatus(request.getDeviceId(), ConfigStatus.ACTIVE)) {
            log.warn("Active config already exists for device: {}", request.getDeviceId());
            throw new ConfigAlreadyExistsException(request.getDeviceId());
        }

        ServerSelectionRequest selectionRequest = ServerSelectionRequest.builder()
                .userId(request.getUserId())
                .preferredCountry(request.getPreferredCountry())
                .userLocation(request.getUserLocation() != null ? request.getUserLocation() : "RU")
                .protocol(request.getProtocol())
                .build();

        ServerSelectionResult serverResult = serverSelectionService.selectBestServer(selectionRequest);
        ServerDto selectedServer = serverResult.getServer();

        log.info("Selected server: {} (score: {:.2f})",
                selectedServer.getName(), serverResult.getTotalScore());

        UUID vlessUuid = uuidGenerator.generateDeterministicUuid(
                request.getUserId(),
                request.getDeviceId()
        );

        String publicKey = selectedServer.getRealityPublicKey() != null
                ? selectedServer.getRealityPublicKey()
                : realityGenerator.generatePublicKey();

        String shortId = selectedServer.getRealityShortId() != null
                ? selectedServer.getRealityShortId()
                : realityGenerator.generateShortId();

        ServerAddress serverAddress = new ServerAddress(selectedServer.getIpAddress());

        String vlessLink = vlessLinkBuilder.buildVlessLink(
                vlessUuid,
                vlessUuid.toString(),
                serverAddress,
                selectedServer.getPort(),
                selectedServer.getName(),
                publicKey,
                shortId
        );

        log.debug("Generated VLESS link: {}",
                vlessLink.length() > 50 ? vlessLink.substring(0, 50) + "..." : vlessLink);

        String qrCodeBase64 = qrCodeGenerator.generateQRCodeBase64(vlessLink);
        String qrCodeDataUrl = qrCodeGenerator.generateQRCodeDataUrl(vlessLink);

        VpnConfiguration config = VpnConfiguration.builder()
                .deviceId(request.getDeviceId())
                .userId(request.getUserId())
                .serverId(selectedServer.getId())
                .vlessUuid(vlessUuid)
                .vlessLink(vlessLink)
                .qrCodeBase64(qrCodeBase64)
                .protocol(ProtocolType.VLESS)
                .status(ConfigStatus.ACTIVE)
                .build();

        VpnConfiguration savedConfig = configRepository.save(config);

        ConfigMetadataDto meta = ConfigMetadataDto.builder()
                .configId(savedConfig.getId())
                .userId(savedConfig.getUserId())
                .deviceId(savedConfig.getDeviceId())
                .build();

        redisCacheService.set("vpn:meta:" + vlessUuid, meta, Duration.ofDays(30));

        log.info("VPN config created: id={}, device={}, server={}, uuid={}",
                savedConfig.getId(),
                savedConfig.getDeviceId(),
                selectedServer.getName(),
                vlessUuid.toString().substring(0, 8) + "...");

        VpnConfigResponse response = configMapper.toResponse(savedConfig, selectedServer, qrCodeDataUrl);
        response.setSelectionReason(serverResult.getSelectionReason());
        response.setServerScore(serverResult.getTotalScore());

        return response;

    }

    @Override
    @Cacheable(value = "vpn-configs", key = "#deviceId")
    public VpnConfigResponse getConfigByDeviceId(Long deviceId) {
        log.debug("Fetching config for device: {}", deviceId);

        VpnConfiguration config = configRepository
                .findByDeviceIdAndStatus(deviceId, ConfigStatus.ACTIVE)
                .orElseThrow(() -> new ConfigNotFoundException(deviceId));

        ServerDto server = serverSelectionService.getAllActiveServers()
                .stream()
                .filter(s -> s.getId().equals(config.getServerId()))
                .findFirst()
                .orElse(null);

        String qrDataUrl = qrCodeGenerator.generateQRCodeDataUrl(config.getVlessLink());

        return configMapper.toResponse(config, server, qrDataUrl);
    }

    @Override
    public VpnConfigResponse getConfigByVlessUuid(UUID vlessUuid) {
        log.debug("Fetching config by VLESS UUID: {}", vlessUuid);

        VpnConfiguration config = configRepository.findByVlessUuid(vlessUuid)
                .orElseThrow(() -> new ConfigNotFoundException("Config not found with UUID: " + vlessUuid));

        ServerDto server = serverSelectionService.getAllActiveServers()
                .stream()
                .filter(s -> s.getId().equals(config.getServerId()))
                .findFirst()
                .orElse(null);

        return configMapper.toResponse(config, server);
    }

    @Override
    @Transactional
    @CachePut(value = "vpn-configs", key = "#deviceId")
    public VpnConfigResponse regenerateConfig(Long deviceId, ConfigRegenerateRequest request) {
        log.info("Regenerating config for device: {}, reason: {}", deviceId, request.getReason());

        VpnConfiguration oldConfig = configRepository
                .findByDeviceIdAndStatus(deviceId, ConfigStatus.ACTIVE)
                .orElseThrow(() -> new ConfigNotFoundException(deviceId));

        oldConfig.revoke();
        configRepository.save(oldConfig);

        log.info("Old config revoked: uuid={}", oldConfig.getVlessUuid());

        ConfigCreateRequest createRequest = ConfigCreateRequest.builder()
                .userId(oldConfig.getUserId())
                .deviceId(deviceId)
                .preferredCountry(request.getPreferredCountry())
                .userLocation("RU") // TODO: Получить из профиля пользователя
                .build();

        return createConfig(createRequest);
    }

    @Override
    @Transactional
    @CacheEvict(value = "vpn-configs", key = "#deviceId")
    public void revokeConfig(Long deviceId, Long userId) {
        log.warn("Revoking config: device={}, user={}", deviceId, userId);

        VpnConfiguration config = configRepository
                .findByDeviceIdAndStatus(deviceId, ConfigStatus.ACTIVE)
                .orElseThrow(() -> new ConfigNotFoundException(deviceId));

        if (!config.getUserId().equals(userId)) {
            log.error("Unauthorized revoke attempt: device={}, userId={}, actualOwner={}",
                    deviceId, userId, config.getUserId());
            throw new UnauthorizedConfigAccessException(deviceId, userId);
        }

        config.revoke();
        configRepository.save(config);

        log.info("Config revoked: device={}, uuid={}", deviceId, config.getVlessUuid());
    }

    @Override
    public List<VpnConfigResponse> getActiveConfigs(Long userId) {
        log.debug("Fetching active configs for user: {}", userId);

        List<VpnConfiguration> configs = configRepository
                .findByUserIdAndStatus(userId, ConfigStatus.ACTIVE);

        List<ServerDto> servers = serverSelectionService.getAllActiveServers();

        return configs.stream()
                .map(config -> {
                    ServerDto server = servers.stream()
                            .filter(s -> s.getId().equals(config.getServerId()))
                            .findFirst()
                            .orElse(null);

                    return configMapper.toResponse(config, server);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateLastUsed(UUID vlessUuid) {
        log.trace("Updating last used for UUID: {}", vlessUuid);

        configRepository.findByVlessUuid(vlessUuid)
                .ifPresent(config -> {
                    config.updateLastUsed();
                    configRepository.save(config);
                });
    }

    @Override
    public boolean isConfigOwnedByUser(Long deviceId, Long userId) {
        return configRepository.findByDeviceIdAndStatus(deviceId, ConfigStatus.ACTIVE)
                .map(config -> config.getUserId().equals(userId))
                .orElse(false);
    }
}
