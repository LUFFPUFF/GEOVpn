package com.vpn.common.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INTERNAL_SERVER_ERROR("SYS_001", "Internal server error occurred"),
    INVALID_REQUEST("SYS_002", "Invalid request format or parameters"),
    UNAUTHORIZED("SYS_003", "Authentication required"),
    FORBIDDEN("SYS_004", "Access denied"),
    METHOD_NOT_ALLOWED("SYS_005", "HTTP method not allowed"),

    USER_NOT_FOUND("USR_001", "User not found"),
    DUPLICATE_USER("USR_002", "User already exists"),
    MAX_DEVICES_EXCEEDED("USR_003", "Maximum number of devices exceeded"),

    CONFIG_GENERATION_FAILED("CFG_001", "Failed to generate VPN configuration"),
    NO_AVAILABLE_SERVERS("CFG_002", "No available servers found");

    private final String code;
    private final String defaultMessage;

}
