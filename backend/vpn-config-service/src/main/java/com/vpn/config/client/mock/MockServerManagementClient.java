package com.vpn.config.client.mock;

import com.vpn.config.client.ServerManagementClient;
import com.vpn.common.dto.ServerDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Mock implementation для локального тестирования
 * Включается через: mock.server-management.enabled=true
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "mock.server-management.enabled", havingValue = "true")
public class MockServerManagementClient implements ServerManagementClient {

    @Override
    public List<ServerDto> getActiveServers() {
        log.debug("Using MOCK Server Management Client");

        return Arrays.asList(
                createMockServer(1, "Riga-1", "LV", "185.230.127.10", 15, 30.0, 95.0),
                createMockServer(2, "Riga-2", "LV", "185.230.127.11", 18, 45.0, 93.0),
                createMockServer(3, "Helsinki-1", "FI", "95.216.89.45", 25, 25.0, 97.0),
                createMockServer(4, "Helsinki-2", "FI", "95.216.89.46", 28, 60.0, 90.0),
                createMockServer(5, "Amsterdam-1", "NL", "37.27.15.20", 35, 40.0, 92.0),
                createMockServer(6, "Amsterdam-2", "NL", "37.27.15.21", 38, 55.0, 88.0)
        );
    }

    @Override
    public ServerDto getServerById(Integer id) {
        log.debug("MOCK: Getting server by ID: {}", id);

        return getActiveServers().stream()
                .filter(s -> s.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    @Override
    public ServerDto getServerHealth(Integer id) {
        log.debug("MOCK: Getting server health for ID: {}", id);
        return getServerById(id);
    }

    private ServerDto createMockServer(
            Integer id,
            String name,
            String country,
            String ip,
            Integer latency,
            Double loadPercentage,
            Double healthScore) {

        int maxConnections = 1000;
        int currentConnections = (int) (maxConnections * loadPercentage / 100);

        return ServerDto.builder()
                .id(id)
                .name(name)
                .location(name + " Location")
                .countryCode(country)
                .ipAddress(ip)
                .port(443)
                .realityPublicKey("MOCK_" + generateMockPublicKey(id))
                .realityShortId(generateMockShortId(id))
                .realitySni("www.google.com")
                .isActive(true)
                .maxConnections(maxConnections)
                .currentConnections(currentConnections)
                .avgLatencyMs(latency)
                .healthScore(healthScore)
                .lastHealthCheck(LocalDateTime.now())
                .build();
    }

    private String generateMockPublicKey(Integer id) {
        return java.util.Base64.getEncoder()
                .encodeToString(("MockPublicKey" + id).getBytes());
    }

    private String generateMockShortId(Integer id) {
        return String.format("mock%04d%08x", id, System.currentTimeMillis() % 0x100000000L)
                .substring(0, 16);
    }
}
