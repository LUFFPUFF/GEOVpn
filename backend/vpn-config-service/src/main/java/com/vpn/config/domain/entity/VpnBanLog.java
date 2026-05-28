package com.vpn.config.domain.entity;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "vpn_ban_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VpnBanLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    @Column(name = "banned_at", nullable = false)
    private LocalDateTime bannedAt;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Column(name = "visited_domains", columnDefinition = "TEXT")
    private String visitedDomains;

    @Column(name = "traffic_consumed_mb")
    private Long trafficConsumedMb;

    @Column(name = "status")
    private String status;
}
