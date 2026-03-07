package com.vpn.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO для получения информации о сервере
 * Используется для inter-service communication
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerDto {

    private Integer id;
    private String name;
    private String location;
    private String countryCode;
    private String ipAddress;
    private Integer port;

    private String realityPublicKey;
    private String realityShortId;
    private String realitySni;

    private Boolean isActive;
    private Integer maxConnections;
    private Integer currentConnections;

    private Integer avgLatencyMs;
    private Double healthScore;
    private LocalDateTime lastHealthCheck;

    /**
     * Рассчитать процент загрузки сервера
     */
    public double getLoadPercentage() {
        if (maxConnections == null || maxConnections == 0) {
            return 0.0;
        }
        return (double) currentConnections / maxConnections * 100.0;
    }

    /**
     * Проверить что сервер доступен
     */
    public boolean isAvailable() {
        return isActive != null && isActive
                && currentConnections != null
                && maxConnections != null
                && currentConnections < maxConnections;
    }

}
