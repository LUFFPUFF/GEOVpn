package com.vpn.server.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class SystemHealthDto {
    private String status;
    private Map<String, ServiceHealth> services;

    @Data
    @Builder
    public static class ServiceHealth {
        private String status;
        private String details;
        private Double cpuUsage;
        private Long memoryUsedMb;
        private Long memoryMaxMb;
        private Long uptime;
        private List<String> recentLogs;
    }
}
