package com.vpn.common.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * VPN конфигурация с поддержкой множественных протоколов
 *
 * Поддерживаемые протоколы:
 * 1. VLESS+Reality (primary) - для обычного использования
 * 2. Shadowsocks (fallback #1) - если Reality заблокирован
 * 3. Hysteria2 (fallback #2) - для мобильных сетей и при глушении
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VpnConfigResponse {

    private Long id;
    private Long deviceId;
    private Long userId;
    private Integer serverId;
    private ServerInfo server;

    /**
     * VLESS ссылка (для быстрого импорта)
     * Работает везде, но может быть заблокирован DPI
     */
    @JsonProperty("vlessLink")
    private String vlessLink;

    /**
     * URL для скачивания полного JSON конфига (с routing rules)
     * Содержит smart routing: российские сайты → direct, заблокированные → proxy
     */
    @JsonProperty("fullConfigUrl")
    private String fullConfigUrl;

    /**
     * Shadowsocks ссылка
     * TCP-based, работает с obfuscation
     * Используй если VLESS заблокирован
     */
    @JsonProperty("shadowsocksLink")
    private String shadowsocksLink;

    /**
     * Shadowsocks JSON конфигурация
     */
    @JsonProperty("shadowsocksConfig")
    private Object shadowsocksConfig;

    /**
     * Hysteria2 ссылка
     * UDP-based (QUIC), оптимизирован для мобильных сетей
     * ЛУЧШИЙ ВАРИАНТ при глушении и на LTE/5G
     */
    @JsonProperty("hysteria2Link")
    private String hysteria2Link;

    /**
     * Hysteria2 JSON конфигурация
     */
    @JsonProperty("hysteria2Config")
    private Object hysteria2Config;

    /**
     * QR код (по умолчанию для VLESS)
     */
    @JsonProperty("qrCode")
    private String qrCode;

    /**
     * Статус конфигурации
     */
    private String status;

    /**
     * Причина выбора сервера
     */
    @JsonProperty("selectionReason")
    private String selectionReason;

    /**
     * Оценка сервера
     */
    @JsonProperty("serverScore")
    private Double serverScore;

    /**
     * Доступные протоколы
     * Пример: ["VLESS", "Shadowsocks", "Hysteria2"]
     */
    @JsonProperty("availableProtocols")
    private java.util.List<String> availableProtocols;

    /**
     * Рекомендуемый протокол
     * Логика:
     * - Для России + мобильные сети → Hysteria2
     * - Для обычного использования → VLESS
     * - При блокировках → Shadowsocks/Hysteria2
     */
    @JsonProperty("recommendedProtocol")
    private String recommendedProtocol;

    /**
     * Информация о сервере
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