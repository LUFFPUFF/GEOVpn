package com.vpn.common.dto.response;


import com.vpn.common.dto.enums.ConfigStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Ответ с VPN конфигурацией
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VpnConfigResponse {

    private Long id;
    private Long deviceId;
    private Long userId;

    private Integer serverId;
    private String serverName;
    private String serverLocation;
    private String serverCountryCode;

    private UUID vlessUuid;
    private String vlessLink;
    private String qrCodeBase64;
    private String qrCodeDataUrl;

    private com.vpn.common.dto.enums.ProtocolType protocol;
    private ConfigStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
    private LocalDateTime revokedAt;

    private String selectionReason;
    private Double serverScore;
}
