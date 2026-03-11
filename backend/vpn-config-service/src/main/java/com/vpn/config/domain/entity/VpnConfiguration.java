package com.vpn.config.domain.entity;

import com.vpn.common.dto.enums.ConfigStatus;
import com.vpn.common.dto.enums.ProtocolType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * История выданных VPN конфигураций
 */
@Entity
@Table(name = "vpn_configurations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VpnConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "server_id", nullable = false)
    private Integer serverId;

    @Column(name = "vless_uuid", nullable = false, unique = true)
    private UUID vlessUuid;

    @Column(name = "vless_link", nullable = false, columnDefinition = "TEXT")
    private String vlessLink;

    @Column(name = "qr_code_base64", columnDefinition = "TEXT")
    private String qrCodeBase64;

    @Enumerated(EnumType.STRING)
    @Column(name = "protocol", nullable = false)
    @Builder.Default
    private ProtocolType protocol = ProtocolType.VLESS;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ConfigStatus status = ConfigStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    /**
     * Отозвать конфигурацию
     */
    public void revoke() {
        this.status = ConfigStatus.REVOKED;
        this.revokedAt = LocalDateTime.now();
    }

    /**
     * Приостановить конфигурацию
     */
    public void suspend() {
        this.status = ConfigStatus.SUSPENDED;
    }

    /**
     * Активировать конфигурацию
     */
    public void activate() {
        this.status = ConfigStatus.ACTIVE;
        this.revokedAt = null;
    }

    /**
     * Обновить время последнего использования
     */
    public void updateLastUsed() {
        this.lastUsedAt = LocalDateTime.now();
    }

    /**
     * Проверить активна ли конфигурация
     */
    public boolean isActive() {
        return this.status == ConfigStatus.ACTIVE;
    }
}
