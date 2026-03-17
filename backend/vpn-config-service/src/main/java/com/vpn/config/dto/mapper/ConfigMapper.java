package com.vpn.config.dto.mapper;

import com.vpn.config.domain.entity.VpnConfiguration;
import com.vpn.common.dto.ServerDto;
import com.vpn.common.dto.response.VpnConfigResponse;
import org.springframework.stereotype.Component;

@Component
public class ConfigMapper {

    public VpnConfigResponse toResponse(VpnConfiguration config, ServerDto server, String qrDataUrl) {
        return VpnConfigResponse.builder()
                .id(config.getId())
                .deviceId(config.getDeviceId())
                .userId(config.getUserId())
                .serverId(config.getServerId())
                .server(mapToServerInfo(server))
                .vlessLink(config.getVlessLink())
                .qrCode(qrDataUrl)
                .status(config.getStatus().name())
                .fullConfigUrl("/api/v1/configs/" + config.getId() + "/json")
                .build();
    }

    private VpnConfigResponse.ServerInfo mapToServerInfo(ServerDto server) {
        if (server == null) {
            return VpnConfigResponse.ServerInfo.builder().name("Unknown").build();
        }
        return VpnConfigResponse.ServerInfo.builder()
                .id(server.getId())
                .name(server.getName())
                .location(server.getLocation())
                .countryCode(server.getCountryCode())
                .ipAddress(server.getIpAddress())
                .port(server.getPort())
                .currentConnections(server.getCurrentConnections())
                .maxConnections(server.getMaxConnections())
                .avgLatencyMs(server.getAvgLatencyMs())
                .healthScore(server.getHealthScore())
                .build();
    }

    public VpnConfigResponse toResponse(VpnConfiguration config, ServerDto server) {
        return toResponse(config, server, null);
    }
}
