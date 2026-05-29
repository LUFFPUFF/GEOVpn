package com.vpn.server.service;

import com.vpn.server.dto.SystemHealthDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemHealthService {

    private final RestTemplate restTemplate;

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @Value("${SERVER_MGM_SERVICE_URL:http://localhost:8084}")
    private String serverMgmUrl;

    @Value("${USER_SERVICE_URL:http://localhost:8082}")
    private String userUrl;

    @Value("${VPN_CONFIG_SERVICE_URL:http://localhost:8083}")
    private String configUrl;

    @Value("${BILLING_SERVICE_URL:http://localhost:8085}")
    private String billingUrl;

    private Map<String, String> serviceUrls;

    @PostConstruct
    public void initServiceUrls() {
        this.serviceUrls = Map.of(
                "server-management", serverMgmUrl.trim() + "/actuator",
                "user-service", userUrl.trim() + "/actuator",
                "vpn-config-service", configUrl.trim() + "/actuator",
                "billing-service", billingUrl.trim() + "/actuator"
        );
        log.info("[HEALTH CONFIG] Initialized service health endpoints mapping: {}", serviceUrls);
    }

    public SystemHealthDto getClusterHealth() {
        Map<String, SystemHealthDto.ServiceHealth> servicesReport = new HashMap<>();

        List<CompletableFuture<Void>> futures = serviceUrls.entrySet().stream()
                .map(entry -> CompletableFuture.runAsync(() -> {
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
                        }

                        synchronized (servicesReport) {
                            servicesReport.put(name, builder.build());
                        }

                    } catch (Exception e) {
                        log.error("Failed to fetch health for service {}: {}", name, e.getMessage());
                        synchronized (servicesReport) {
                            servicesReport.put(name, SystemHealthDto.ServiceHealth.builder()
                                    .status("DOWN")
                                    .details("Service unreachable")
                                    .build());
                        }
                    }
                }, executor))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        boolean isAllUp = servicesReport.values().stream()
                .allMatch(h -> "UP".equals(h.getStatus()));

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

    @jakarta.annotation.PreDestroy
    public void shutdown() {
        executor.shutdown();
    }
}