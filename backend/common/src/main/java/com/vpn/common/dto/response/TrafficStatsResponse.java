package com.vpn.common.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TrafficStatsResponse {

    @JsonProperty("userId")
    private Long userId;

    @JsonProperty("totalBytesIn")
    private Long totalBytesIn;

    @JsonProperty("totalBytesOut")
    private Long totalBytesOut;

    @JsonProperty("totalBytes")
    private Long totalBytes;

    @JsonProperty("totalGb")
    private Double totalGb;

    @JsonProperty("totalCostKopecks")
    private Long totalCostKopecks;

    @JsonProperty("totalCostRubles")
    private Double totalCostRubles;

    @JsonProperty("sessions")
    private List<TrafficSession> sessions;

    @Data
    @Builder
    public static class TrafficSession {
        private Integer serverId;
        private Long bytesIn;
        private Long bytesOut;
        private Long bytesTotal;
        private Long costKopecks;
        private LocalDateTime collectedAt;
    }
}
