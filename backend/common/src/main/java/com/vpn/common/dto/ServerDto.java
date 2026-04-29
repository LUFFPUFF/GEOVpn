package com.vpn.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServerDto {

    private Integer id;
    private String name;
    private String location;

    @JsonProperty("country_code")
    private String countryCode;

    @JsonProperty("ip_address")
    private String ipAddress;

    private Integer port;

    @JsonProperty("reality_public_key")
    private String realityPublicKey;

    @JsonProperty("reality_short_id")
    private String realityShortId;

    @JsonProperty("reality_sni")
    private String realitySni;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("max_connections")
    private Integer maxConnections;

    @JsonProperty("current_connections")
    private Integer currentConnections;

    @JsonProperty("avg_latency_ms")
    private Integer avgLatencyMs;

    @JsonProperty("health_score")
    private Double healthScore;

    @JsonProperty("last_health_check")
    private LocalDateTime lastHealthCheck;

    @JsonProperty("relay_priority")
    private Integer relayPriority;

    @JsonProperty("is_relay")
    private boolean isRelay;

    @JsonProperty("relay_sni")
    private String relaySni;

    @JsonProperty("relay_public_key")
    private String relayPublicKey;

    @JsonProperty("relay_short_id")
    private String relayShortId;

    @JsonProperty("panel_username")
    private String panelUsername;

    @JsonProperty("panel_password")
    private String panelPassword;

    @JsonProperty("panel_inbound_id")
    private Integer panelInboundId;

    @JsonProperty("panel_path")
    private String panelPath;

    @JsonProperty("panel_port")
    private Integer panelPort;

    @JsonProperty("grpc_port")
    private Integer grpcPort;

    public double getLoadPercentage() {
        if (maxConnections == null || maxConnections == 0) return 0.0;
        return (double) currentConnections / maxConnections * 100.0;
    }

    public boolean isAvailable() {
        return isActive != null && isActive
                && currentConnections != null
                && maxConnections != null
                && currentConnections < maxConnections;
    }
}