package com.vpn.user.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "devices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "device_name", nullable = false)
    private String deviceName;

    @Column(name = "device_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private DeviceType deviceType;

    @Column(name = "uuid", nullable = false, unique = true)
    private UUID uuid;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "last_connected_at")
    private LocalDateTime lastConnectedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void updateLastConnected() {
        this.lastConnectedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.isActive = false;
    }

}
