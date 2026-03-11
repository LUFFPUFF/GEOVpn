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
                .serverName(server != null ? server.getName() : "Unknown")
                .serverLocation(server != null ? server.getLocation() : "Unknown")
                .serverCountryCode(server != null ? server.getCountryCode() : "Unknown")
                .vlessUuid(config.getVlessUuid())
                .vlessLink(config.getVlessLink())
                .qrCodeBase64(config.getQrCodeBase64())
                .qrCodeDataUrl(qrDataUrl)
                .protocol(config.getProtocol())
                .status(config.getStatus())
                .createdAt(config.getCreatedAt())
                .lastUsedAt(config.getLastUsedAt())
                .revokedAt(config.getRevokedAt())
                .build();
    }

    public VpnConfigResponse toResponse(VpnConfiguration config, ServerDto server) {
        return toResponse(config, server, null);
    }
}
