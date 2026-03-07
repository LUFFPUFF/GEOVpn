package com.vpn.server.service;

import com.vpn.server.domain.entity.Server;
import com.vpn.server.grpc.XrayGrpcClient;
import com.vpn.server.repository.ServerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import com.vpn.server.util.NetworkUtils;

@ExtendWith(MockitoExtension.class)
class ServerMonitoringServiceTest {

    @Mock private ServerRepository serverRepository;
    @Mock private XrayGrpcClient xrayGrpcClient;
    @Mock private NetworkUtils networkUtils;

    @InjectMocks
    private ServerMonitoringService monitoringService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(monitoringService, "xrayGrpcPort", 10085);
        ReflectionTestUtils.setField(monitoringService, "timeoutMs", 100);
    }

    @Test
    @DisplayName("Проверка работоспособности: Сервер РАБОТОСПОСОБЕН (пинг в порядке + Xray в порядке)")
    void shouldMarkServerAsHealthy() {
        printHeader("TEST: HEALTHY SERVER SCENARIO");

        Server server = createServer("FI-Helsinki", "95.10.10.10", 100.0);

        when(networkUtils.isPortOpen(anyString(), anyInt(), anyInt())).thenReturn(true);
        printStep("TCP Check", "Port 443 is OPEN");

        XrayGrpcClient.SysMetrics metrics = XrayGrpcClient.SysMetrics.builder()
                .numGoroutine(500)
                .uptime(123456L)
                .build();
        when(xrayGrpcClient.getSysMetrics(anyString(), anyInt())).thenReturn(metrics);
        printStep("gRPC Check", "Xray is ALIVE. Goroutines: " + metrics.getNumGoroutine());

        monitoringService.checkServer(server);

        printResult("Server Status: " + (server.getHealthScore() > 90 ? "EXCELLENT" : "GOOD"));

        assertThat(server.getHealthScore()).isEqualTo(100.0);
        assertThat(server.getCurrentConnections()).isEqualTo(125);
        verify(serverRepository).save(server);
    }

    @Test
    @DisplayName("Проверка работоспособности: Сервер доступен, но Xray недоступен.")
    void shouldMarkXrayDown() {
        printHeader("TEST: XRAY CRASH SCENARIO");

        Server server = createServer("NL-Amsterdam", "80.80.80.80", 90.0);

        when(networkUtils.isPortOpen(anyString(), anyInt(), anyInt())).thenReturn(true);
        printStep("TCP Check", "Port 443 is OPEN");

        when(xrayGrpcClient.getSysMetrics(anyString(), anyInt())).thenReturn(null);
        printStep("gRPC Check", "Xray is NOT RESPONDING (null metrics)");

        monitoringService.checkServer(server);

        printResult("Server Status: DEGRADED (Health Score: " + server.getHealthScore() + ")");

        assertThat(server.getHealthScore()).isEqualTo(70.0);
        verify(serverRepository).save(server);
    }

    @Test
    @DisplayName("Проверка работоспособности: Сервер отключен (таймаут пинга)")
    void shouldMarkServerOffline() {
        printHeader("TEST: SERVER OFFLINE SCENARIO");

        Server server = createServer("LV-Riga", "1.2.3.4", 50.0);

        when(networkUtils.isPortOpen(anyString(), anyInt(), anyInt())).thenReturn(false);
        printStep("TCP Check", "Port 443 is CLOSED (Timeout)");

        monitoringService.checkServer(server);

        printResult("Server Status: CRITICAL (Health Score: " + server.getHealthScore() + ")");

        assertThat(server.getHealthScore()).isEqualTo(30.0);
        assertThat(server.getAvgLatencyMs()).isNull();
        verify(xrayGrpcClient, never()).getSysMetrics(anyString(), anyInt());
    }

    private Server createServer(String name, String ip, Double score) {
        return Server.builder()
                .name(name)
                .ipAddress(ip)
                .port(443)
                .healthScore(score)
                .currentConnections(0)
                .build();
    }

    private void printHeader(String title) {
        System.out.println("\n╔" + "═".repeat(60) + "╗");
        System.out.printf("║ %-58s ║\n", title);
        System.out.println("╚" + "═".repeat(60) + "╝");
    }

    private void printStep(String step, String details) {
        System.out.printf("  %-12s : %s\n", step, details);
    }

    private void printResult(String message) {
        System.out.println("  " + "─".repeat(58));
        System.out.println(message + "\n");
    }
}