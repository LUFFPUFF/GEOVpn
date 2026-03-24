package com.vpn.user.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "connections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Connection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "server_id")
    private Long serverId;

    @Column(name = "bytes_sent")
    private Long bytesSent;

    @Column(name = "bytes_received")
    private Long bytesReceived;

    @Column(name = "connected_at")
    private LocalDateTime connectedAt;

    @Column(name = "disconnected_at")
    private LocalDateTime disconnectedAt;
}
