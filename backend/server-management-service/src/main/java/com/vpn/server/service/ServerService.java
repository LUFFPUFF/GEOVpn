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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServerService {

    private final ServerRepository serverRepository;
    private final ServerMapper serverMapper;

    @Transactional
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
    public ServerDto updateServer(Integer id, UpdateServerRequest request) {
        Server server = serverRepository.findById(id)
                .orElseThrow(() -> new BaseException(
                        ErrorCode.SERVER_NOT_FOUND,
                        "Server not found: " + id
                ));

        if (request.getLocation() != null) {
            server.setLocation(request.getLocation());
        }
        if (request.getMaxConnections() != null) {
            server.setMaxConnections(request.getMaxConnections());
        }
        if (request.getIsActive() != null) {
            server.setIsActive(request.getIsActive());
        }

        Server updated = serverRepository.save(server);
        return serverMapper.toDto(updated);
    }

    @Transactional
    public void deleteServer(Integer id) {
        Server server = serverRepository.findById(id)
                .orElseThrow(() -> new BaseException(
                        ErrorCode.SERVER_NOT_FOUND,
                        "Server not found: " + id
                ));

        server.setIsActive(false);
        serverRepository.save(server);
    }

    public List<ServerDto> getAllActiveServers() {
        return serverMapper.toDtoList(serverRepository.findByIsActiveTrue());
    }

    public ServerDto getServerById(Integer id) {
        Server server = serverRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.SERVER_NOT_FOUND, "Server not found: " + id));
        return serverMapper.toDto(server);
    }
}
