package com.vpn.server.service;

import com.vpn.server.dto.SystemHealthDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemHealthService {

    private final RestTemplate restTemplate = new RestTemplate();

    private final Map<String, String> serviceUrls = Map.of(
            "server-management", "http://localhost:8084/actuator",
            "user-service", "http://localhost:8082/actuator",
            "vpn-config-service", "http://localhost:8083/actuator"
    );

    public SystemHealthDto getClusterHealth() {
        Map<String, SystemHealthDto.ServiceHealth> servicesReport = new HashMap<>();
        boolean isAllUp = true;

        for (Map.Entry<String, String> entry : serviceUrls.entrySet()) {
            String name = entry.getKey();
            String baseUrl = entry.getValue();

            try {
                Map health = restTemplate.getForObject(baseUrl + "/health", Map.class);
                String status = (health != null) ? (String) health.get("status") : "DOWN";

                SystemHealthDto.ServiceHealth.ServiceHealthBuilder builder = SystemHealthDto.ServiceHealth.builder()
                        .status(status)
                        .details(status.equals("UP") ? "All systems operational" : "Service issue detected");

                if ("UP".equals(status)) {
                    builder.cpuUsage(getMetricValue(baseUrl, "system.cpu.usage") * 100);
                    builder.memoryUsedMb((long) (getMetricValue(baseUrl, "jvm.memory.used") / 1024 / 1024));
                    builder.memoryMaxMb((long) (getMetricValue(baseUrl, "jvm.memory.max") / 1024 / 1024));
                    builder.uptime(getMetricValue(baseUrl, "process.uptime").longValue());
                } else {
                    isAllUp = false;
                }

                servicesReport.put(name, builder.build());

            } catch (Exception e) {
                log.error("Failed to fetch health for service {}: {}", name, e.getMessage());
                servicesReport.put(name, SystemHealthDto.ServiceHealth.builder()
                        .status("DOWN")
                        .details("Service unreachable")
                        .build());
                isAllUp = false;
            }
        }

        return SystemHealthDto.builder()
                .status(isAllUp ? "UP" : "DEGRADED")
                .services(servicesReport)
                .build();
    }

    private Double getMetricValue(String baseUrl, String metricName) {
        try {
            Map response = restTemplate.getForObject(baseUrl + "/metrics/" + metricName, Map.class);
            if (response != null && response.containsKey("measurements")) {
                List<Map<String, Object>> measurements = (List<Map<String, Object>>) response.get("measurements");
                return Double.valueOf(measurements.get(0).get("value").toString());
            }
        } catch (Exception ignored) {}
        return 0.0;
    }
}
