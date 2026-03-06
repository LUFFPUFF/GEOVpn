package com.vpn.config.domain.enums;

public enum ConfigStatus {
    ACTIVE,      // Активна и используется
    REVOKED,     // Отозвана пользователем
    EXPIRED,     // Истекла (если есть TTL)
    SUSPENDED    // Приостановлена
}
