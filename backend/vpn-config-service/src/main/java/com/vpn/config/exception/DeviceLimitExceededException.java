package com.vpn.config.exception;

public class DeviceLimitExceededException extends RuntimeException {

    private final Long userId;
    private final int activeDevices;
    private final int maxDevices;

    public DeviceLimitExceededException(
            Long userId, int activeDevices, int maxDevices) {
        super(String.format(
                "Device limit exceeded for user %d: %d/%d devices active",
                userId, activeDevices, maxDevices));
        this.userId = userId;
        this.activeDevices = activeDevices;
        this.maxDevices = maxDevices;
    }

    public Long getUserId() { return userId; }
    public int getActiveDevices() { return activeDevices; }
    public int getMaxDevices() { return maxDevices; }
}
