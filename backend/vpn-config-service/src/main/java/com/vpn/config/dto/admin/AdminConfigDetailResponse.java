package com.vpn.config.dto.admin;
import com.vpn.config.domain.valueobject.StoredVpnLinks;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class AdminConfigDetailResponse {
    private Long id;
    private Long deviceId;
    private Long userId;
    private UUID vlessUuid;
    private String status;
    private String deviceOs;
    private String deviceName;

    private List<StoredVpnLinks.DirectLink> vlessLinks;
    private List<StoredVpnLinks.RelayLink> relayLinks;
    private List<String> hy2Links;
}
