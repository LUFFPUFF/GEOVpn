package com.vpn.common.dto.response;

@lombok.Builder
@lombok.Data
public class DeviceLimitStatus {
    private Long userId;
    private int maxDevices;
    private int activeDevices;
    private int remainingSlots;
    private boolean limitReached;
}
