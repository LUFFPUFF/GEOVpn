package com.vpn.server.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "traffic_usage")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrafficUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    @Column(name = "server_id", nullable = false)
    private Integer serverId;

    @Column(name = "config_id", nullable = false)
    private Long configId;

    @Column(name = "bytes_in", nullable = false)
    private Long bytesIn;

    @Column(name = "bytes_out", nullable = false)
    private Long bytesOut;

    @Column(name = "bytes_total", insertable = false, updatable = false)
    private Long bytesTotal;

    @Column(name = "cost_kopecks", nullable = false)
    private Integer costKopecks;

    @CreationTimestamp
    @Column(name = "collected_at", nullable = false, updatable = false)
    private LocalDateTime collectedAt;
}
