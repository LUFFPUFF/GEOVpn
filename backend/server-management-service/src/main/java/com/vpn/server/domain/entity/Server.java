package com.vpn.server.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * VPN-сервер.
 *
 * Два типа серверов:
 *
 * 1. Обычный VPS (is_relay = false) — прямое подключение.
 *    Ссылка: vless://uuid@ipAddress:port?pbk=realityPublicKey&sid=realityShortId&sni=realitySni
 *
 * 2. Relay / антиглушилка (is_relay = true) — трафик идёт через RU-IP,
 *    внутри туннелируется к реальному VPS. Используется когда оператор
 *    глушит прямые зарубежные адреса (мобильный LTE).
 *    Ссылка строится с ip=ipAddress, но pbk/sid/sni берутся из relay*-полей.
 */
@Entity
@Table(name = "servers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Server {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String location;

    @Column(name = "country_code", nullable = false, length = 2, columnDefinition = "bpchar(2)")
    private String countryCode;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(nullable = false)
    private Integer port;

    @Column(name = "reality_public_key")
    private String realityPublicKey;

    @Column(name = "reality_short_id", length = 50)
    private String realityShortId;

    @Column(name = "reality_sni")
    @Builder.Default
    private String realitySni = "www.google.com";

    /**
     * true  → этот сервер является антиглушилкой / relay.
     * false → обычный VPS (default).
     */
    @Column(name = "is_relay", nullable = false)
    @Builder.Default
    private Boolean isRelay = false;

    /**
     * SNI для relay-ссылки (напр. "eh.vk.com").
     * Используется только когда is_relay = true.
     */
    @Column(name = "relay_sni")
    private String relaySni;

    /**
     * Reality public key для relay.
     * Если null и is_relay=true — берётся realityPublicKey.
     */
    @Column(name = "relay_public_key")
    private String relayPublicKey;

    /**
     * Reality short ID для relay.
     * Если null и is_relay=true — берётся realityShortId.
     */
    @Column(name = "relay_short_id", length = 50)
    private String relayShortId;

    /**
     * Порядок отображения relay-серверов в подписке (меньше = выше).
     */
    @Column(name = "relay_priority", nullable = false)
    @Builder.Default
    private Integer relayPriority = 0;


    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "max_connections")
    @Builder.Default
    private Integer maxConnections = 1000;

    @Column(name = "current_connections")
    @Builder.Default
    private Integer currentConnections = 0;

    @Column(name = "avg_latency_ms")
    private Integer avgLatencyMs;

    @Column(name = "health_score", columnDefinition = "numeric")
    @Builder.Default
    private Double healthScore = 100.00;

    @Column(name = "grpc_port")
    private Integer grpcPort;

    @Column(name = "last_health_check")
    private LocalDateTime lastHealthCheck;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "panel_username")
    private String panelUsername;

    @Column(name = "panel_password")
    private String panelPassword;

    @Column(name = "panel_inbound_id")
    private Integer panelInboundId;

    @Column(name = "panel_path")
    private String panelPath;

    @Column(name = "panel_port")
    private Integer panelPort;


    public void updateHealthMetrics(boolean isAlive, int latencyMs, int currentConnections) {
        this.lastHealthCheck = LocalDateTime.now();
        this.currentConnections = currentConnections;

        if (isAlive) {
            this.avgLatencyMs = latencyMs;
            this.healthScore = Math.min(100.0, this.healthScore + 5.0);
        } else {
            this.healthScore = Math.max(0.0, this.healthScore - 20.0);
            this.avgLatencyMs = null;
        }
    }

    /**
     * Процент загрузки сервера (0..100).
     */
    public double getLoadPercentage() {
        if (maxConnections == null || maxConnections == 0) return 0.0;
        return (currentConnections * 100.0) / maxConnections;
    }

    /**
     * Является ли сервер доступным (активен + не переполнен).
     */
    public boolean isAvailable() {
        return Boolean.TRUE.equals(isActive) && getLoadPercentage() < 95.0;
    }

    /**
     * Вернуть effective public key для relay-ссылки:
     * сначала relayPublicKey, потом realityPublicKey.
     */
    public String effectiveRelayPublicKey() {
        return (relayPublicKey != null && !relayPublicKey.isBlank())
                ? relayPublicKey : realityPublicKey;
    }

    /**
     * Вернуть effective short ID для relay-ссылки.
     */
    public String effectiveRelayShortId() {
        return (relayShortId != null && !relayShortId.isBlank())
                ? relayShortId : realityShortId;
    }
}