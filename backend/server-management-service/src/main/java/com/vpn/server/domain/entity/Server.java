package com.vpn.server.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

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

    @Column(name = "last_health_check")
    private LocalDateTime lastHealthCheck;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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
}
