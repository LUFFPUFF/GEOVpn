package com.vpn.common.dto.response;

import com.vpn.common.dto.enums.DeviceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceResponse {

    private Long id;
    private String deviceName;
    private DeviceType deviceType;
    private UUID uuid;
    private Boolean isActive;
    private LocalDateTime lastConnectedAt;
    private LocalDateTime createdAt;
}
