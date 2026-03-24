package com.vpn.common.dto.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConnectionUpdateRequest {
    private Long userId;
    private Long deviceId;
    private Integer serverId;
    private Long deltaUp;
    private Long deltaDown;
}
