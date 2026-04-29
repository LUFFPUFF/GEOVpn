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
                .grpcPort(server.getGrpcPort())
                .isRelay(server.getIsRelay())
                .relayPriority(server.getRelayPriority())
                .relaySni(server.getRelaySni())
                .relayPublicKey(server.getRelayPublicKey())
                .relayShortId(server.getRelayShortId())
                .panelUsername(server.getPanelUsername())
                .panelPassword(server.getPanelPassword())
                .panelInboundId(server.getPanelInboundId())
                .panelPath(server.getPanelPath())
                .panelPort(server.getPanelPort())
                .build();
    }

    public List<ServerDto> toDtoList(List<Server> servers) {
        if (servers == null) {
            return List.of();
        }
        return servers.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
}
