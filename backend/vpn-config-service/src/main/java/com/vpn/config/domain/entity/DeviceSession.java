package com.vpn.config.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Запись о подключении конкретного физического устройства.
 *
 * Идентификатор устройства берётся из заголовка X-Device-Fingerprint
 * (генерируется клиентом Happ на основе HWID).
 *
 * Один пользователь может иметь N активных сессий,
 * где N = DeviceLimit.maxDevices.
 */
@Entity
@Table(
        name = "device_sessions",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_device_fingerprint",
                columnNames = {"user_id", "device_fingerprint"}
        )
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DeviceSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "device_fingerprint", nullable = false, length = 128)
    private String deviceFingerprint;

    @Column(name = "vless_uuid", nullable = false)
    private UUID vlessUuid;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "device_name", length = 256)
    private String deviceName;

    @Column(name = "last_ip", length = 45)
    private String lastIp;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "first_seen_at", updatable = false)
    private LocalDateTime firstSeenAt;

    @UpdateTimestamp
    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

}
