package com.vpn.common.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Запрос на создание VPN конфигурации
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigCreateRequest {

    private Long userId;

    private Long userTelegramId;

    private String username;

    private Long deviceId;

    private String preferredCountry;

    private String userLocation;
    @Builder.Default
    private String protocol = "VLESS";

    private String deviceName;
    private String deviceOs;


}
