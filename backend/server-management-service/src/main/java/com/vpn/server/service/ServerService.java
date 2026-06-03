package com.vpn.server.service;

import com.vpn.common.dto.ServerDto;
import com.vpn.common.exception.BaseException;
import com.vpn.common.constant.ErrorCode;
import com.vpn.server.domain.entity.Server;
import com.vpn.server.dto.CreateServerRequest;
import com.vpn.server.dto.UpdateServerRequest;
import com.vpn.server.dto.mapper.ServerMapper;
import com.vpn.server.repository.ServerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServerService {

    private final ServerRepository serverRepository;
    private final ServerMapper serverMapper;

    @Transactional
    @CacheEvict(value = "servers-all", allEntries = true)
    public ServerDto createServer(CreateServerRequest request) {
        if (serverRepository.findByName(request.getName()).isPresent()) {
            throw new BaseException(
                    ErrorCode.HEALTH_CHECK_FAILED,
                    "Server with name " + request.getName() + " already exists"
            );
        }

        Server server = Server.builder()
                .name(request.getName())
                .location(request.getLocation())
                .countryCode(request.getCountryCode())
                .ipAddress(request.getIpAddress())
                .port(request.getPort())
                .grpcPort(request.getGrpcPort())
                .realityPublicKey(request.getRealityPublicKey())
                .realityShortId(request.getRealityShortId())
                .realitySni(request.getRealitySni())
                .maxConnections(request.getMaxConnections())
                .isActive(true)
                .healthScore(100.0)
                .currentConnections(0)
                .build();

        Server saved = serverRepository.save(server);
        return serverMapper.toDto(saved);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "servers-all", allEntries = true),
            @CacheEvict(value = "servers-active", allEntries = true),
            @CacheEvict(value = "server-single", key = "#id")
    })
    public ServerDto updateServer(Integer id, UpdateServerRequest request) {
        Server server = serverRepository.findById(id)
                .orElseThrow(() -> new BaseException(
                        ErrorCode.SERVER_NOT_FOUND,
                        "Server not found: " + id
                ));

        if (request.getLocation() != null) server.setLocation(request.getLocation());
        if (request.getMaxConnections() != null) server.setMaxConnections(request.getMaxConnections());
        if (request.getIsActive() != null) server.setIsActive(request.getIsActive());

        if (request.getIpAddress() != null) server.setIpAddress(request.getIpAddress());
        if (request.getPort() != null) server.setPort(request.getPort());

        if (request.getRealitySni() != null) server.setRealitySni(request.getRealitySni());
        if (request.getRealityPublicKey() != null) server.setRealityPublicKey(request.getRealityPublicKey());
        if (request.getRealityShortId() != null) server.setRealityShortId(request.getRealityShortId());

        if (request.getRelaySni() != null) server.setRelaySni(request.getRelaySni());
        if (request.getRelayPublicKey() != null) server.setRelayPublicKey(request.getRelayPublicKey());
        if (request.getRelayShortId() != null) server.setRelayShortId(request.getRelayShortId());

        Server updated = serverRepository.save(server);
        return serverMapper.toDto(updated);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "servers-all", allEntries = true),
            @CacheEvict(value = "servers-active", allEntries = true),
            @CacheEvict(value = "server-single", key = "#id")
    })
    public void deleteServer(Integer id) {
        Server server = serverRepository.findById(id)
                .orElseThrow(() -> new BaseException(
                        ErrorCode.SERVER_NOT_FOUND,
                        "Server not found: " + id
                ));

        server.setIsActive(false);
        serverRepository.save(server);
    }

    @Cacheable(value = "servers-active")
    public List<ServerDto> getAllActiveServers() {
        List<Server> servers = serverRepository.findByIsActiveTrue();
        log.info("Returning {} active servers from database (or cache)", servers.size());
        return serverMapper.toDtoList(servers);
    }

    @Cacheable(value = "server-single", key = "#id")
    public ServerDto getServerById(Integer id) {
        Server server = serverRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.SERVER_NOT_FOUND, "Server not found: " + id));
        return serverMapper.toDto(server);
    }

    @Cacheable(value = "servers-all")
    public List<ServerDto> getAllServers() {
        return serverMapper.toDtoList(serverRepository.findAll());
    }
}