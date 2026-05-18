package com.vpn.common.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceLimitStatus {
    private Long userId;
    private int maxDevices;
    private int activeDevices;
    private int remainingSlots;
    private boolean limitReached;
}
