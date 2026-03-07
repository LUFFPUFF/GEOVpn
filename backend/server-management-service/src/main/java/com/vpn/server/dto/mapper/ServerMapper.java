package com.vpn.server.dto.mapper;

import com.vpn.common.dto.ServerDto;
import com.vpn.server.domain.entity.Server;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ServerMapper {

    public ServerDto toDto(Server server) {
        if (server == null) {
            return null;
        }

        return ServerDto.builder()
                .id(server.getId())
                .name(server.getName())
                .location(server.getLocation())
                .countryCode(server.getCountryCode())
                .ipAddress(server.getIpAddress())
                .port(server.getPort())
                .realityPublicKey(server.getRealityPublicKey())
                .realityShortId(server.getRealityShortId())
                .realitySni(server.getRealitySni())
                .isActive(server.getIsActive())
                .maxConnections(server.getMaxConnections())
                .currentConnections(server.getCurrentConnections())
                .avgLatencyMs(server.getAvgLatencyMs())
                .healthScore(server.getHealthScore())
                .lastHealthCheck(server.getLastHealthCheck())
                .build();
    }

    public List<ServerDto> toDtoList(List<Server> servers) {
        return servers.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
}
