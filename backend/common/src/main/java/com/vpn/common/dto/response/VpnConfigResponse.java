package com.vpn.common.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * VPN конфигурация с поддержкой множественных протоколов
 *
 * Поддерживаемые протоколы:
 * 1. VLESS+Reality (primary) - для обычного использования
 * 2. VLESS+Reality+Ru Server (fallback #1) - для мобильных сетей и при глушении
 * 3. Hysteria2 (fallback #2) - fallback если не работает Reality
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VpnConfigResponse {

    private Long id;
    private Long deviceId;
    private Long userId;

    /**
     * URL подписки для импорта в Happ/V2Box
     * Содержит все доступные серверы в base64
     */
    @JsonProperty("subscriptionUrl")
    private String subscriptionUrl;

    /**
     * Список всех конфигураций (для отображения)
     */
    @JsonProperty("configs")
    private List<ServerConfig> configs;

    /**
     * Рекомендуемый протокол
     */
    @JsonProperty("recommendedProtocol")
    private String recommendedProtocol;

    /**
     * Статус
     */
    private String status;

    @JsonProperty("subscriptionBase64")
    private String subscriptionBase64;

    private String qrCode;

    @JsonProperty("selectionReason")
    private String selectionReason;

    @JsonProperty("serverScore")
    private Double serverScore;

    @JsonProperty("availableProtocols")
    private List<String> availableProtocols;


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServerConfig {
        private Integer serverId;
        private String serverName;
        private String countryCode;
        private String countryEmoji;
        private String type;
        private String vlessLink;
        private String protocol;
        private Integer avgLatencyMs;
        private Double healthScore;
        private Boolean isRelay;
    }

    /**
     * Информация о сервере (упрощённая)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServerInfo {
        private Integer id;
        private String name;
        private String location;
        private String countryCode;
        private String ipAddress;
        private Integer port;
        private Integer currentConnections;
        private Integer maxConnections;
        private Integer avgLatencyMs;
        private Double healthScore;
    }
}