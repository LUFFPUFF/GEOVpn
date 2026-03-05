package com.vpn.common.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // System Errors (SYS)
    INTERNAL_SERVER_ERROR("SYS_001", "Internal server error occurred"),
    INVALID_REQUEST("SYS_002", "Invalid request format or parameters"),
    UNAUTHORIZED("SYS_003", "Authentication required"),
    FORBIDDEN("SYS_004", "Access denied"),
    METHOD_NOT_ALLOWED("SYS_005", "HTTP method not allowed"),
    RATE_LIMIT_EXCEEDED("SYS_006", "Too many requests"),

    // User Errors (USR)
    USER_NOT_FOUND("USR_001", "User not found"),
    DUPLICATE_USER("USR_002", "User already exists"),
    MAX_DEVICES_EXCEEDED("USR_003", "Maximum number of devices exceeded"),
    INVALID_TELEGRAM_ID("USR_004", "Invalid Telegram ID"),
    USER_INACTIVE("USR_005", "User account is inactive"),

    // Device Errors (DEV)
    DEVICE_NOT_FOUND("DEV_001", "Device not found"),
    INVALID_DEVICE_TYPE("DEV_002", "Invalid device type"),
    DEVICE_ALREADY_EXISTS("DEV_003", "Device already registered"),
    DEVICE_INACTIVE("DEV_004", "Device is inactive"),

    // VPN Config Errors (CFG)
    CONFIG_GENERATION_FAILED("CFG_001", "Failed to generate VPN configuration"),
    NO_AVAILABLE_SERVERS("CFG_002", "No available servers found"),
    INVALID_CONFIG_FORMAT("CFG_003", "Invalid configuration format"),

    // Server Errors (SRV)
    SERVER_NOT_FOUND("SRV_001", "Server not found"),
    SERVER_UNAVAILABLE("SRV_002", "Server is currently unavailable"),
    SERVER_OVERLOADED("SRV_003", "Server is overloaded"),
    HEALTH_CHECK_FAILED("SRV_004", "Server health check failed"),

    // Billing Errors (BIL)
    INSUFFICIENT_BALANCE("BIL_001", "Insufficient balance"),
    INVALID_SUBSCRIPTION("BIL_002", "Invalid or expired subscription"),
    PAYMENT_FAILED("BIL_003", "Payment processing failed"),
    INVALID_AMOUNT("BIL_004", "Invalid payment amount"),

    // Blocked Domains Errors (BLK)
    DOMAIN_NOT_FOUND("BLK_001", "Domain not found"),
    INVALID_DOMAIN_FORMAT("BLK_002", "Invalid domain format"),
    DOMAIN_LIST_UPDATE_FAILED("BLK_003", "Failed to update blocked domains list");

    private final String code;
    private final String defaultMessage;
}