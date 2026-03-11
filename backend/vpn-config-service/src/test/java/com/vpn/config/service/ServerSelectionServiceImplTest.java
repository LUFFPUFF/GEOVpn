package com.vpn.config.service;

import com.vpn.config.client.ResilientServerManagementClient;
import com.vpn.config.config.VpnConfigProperties;
import com.vpn.common.dto.ServerDto;
import com.vpn.common.dto.request.ServerSelectionRequest;
import com.vpn.common.dto.ServerSelectionResult;
import com.vpn.config.exception.NoAvailableServerException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServerSelectionServiceImplTest {

    @Mock
    private ResilientServerManagementClient client;

    @Mock
    private VpnConfigProperties configProperties;

    @InjectMocks
    private ServerSelectionServiceImpl selectionService;

    @BeforeEach
    void setUp() {
        VpnConfigProperties.SelectionProperties selection = new VpnConfigProperties.SelectionProperties();

        selection.getWeights().setLatency(0.4);
        selection.getWeights().setLoad(0.3);
        selection.getWeights().setGeography(0.2);
        selection.getWeights().setHealth(0.1);
        selection.getWeights().setProtocol(0.0);

        selection.getLatency().setMultiplier(0.5);
        selection.getLatency().setExcellentThreshold(20);
        selection.getLatency().setGoodThreshold(50);
        selection.getLatency().setMaxAcceptableMs(200);

        selection.getLoad().setOptimalThreshold(50.0);
        selection.getLoad().setWarningThreshold(70.0);
        selection.getLoad().setCriticalThreshold(90.0);

        selection.setGeographyBonus(Map.of(
                "EU", Map.of("NL", 20, "FI", 15),
                "default", Map.of("NL", 5)
        ));

        lenient().when(configProperties.getSelection()).thenReturn(selection);
    }

    @Test
    @DisplayName("Выбор оптимального сервера на основе нагрузки и задержки")
    void shouldSelectBestServer() {
        printHeader("TEST: SELECT BEST SERVER ALGORITHM");

        ServerDto nlServer = ServerDto.builder()
                .id(1).name("NL-Amsterdam-1").countryCode("NL")
                .avgLatencyMs(45)
                .maxConnections(1000).currentConnections(200)
                .isActive(true).healthScore(99.0)
                .build();

        ServerDto fiServer = ServerDto.builder()
                .id(2).name("FI-Helsinki-1").countryCode("FI")
                .avgLatencyMs(150)
                .maxConnections(1000).currentConnections(950)
                .isActive(true).healthScore(80.0)
                .build();

        ServerDto offlineServer = ServerDto.builder()
                .id(3).name("LV-Riga-Offline").countryCode("LV")
                .isActive(false)
                .build();

        printStep("Pool", String.format("Available servers: %s, %s, %s", nlServer.getName(), fiServer.getName(), offlineServer.getName()));

        when(client.getActiveServers()).thenReturn(List.of(nlServer, fiServer, offlineServer));

        ServerSelectionRequest request = ServerSelectionRequest.builder()
                .userId(1L)
                .userLocation("DE")
                .build();

        ServerSelectionResult result = selectionService.selectBestServer(request);

        printStep("Result", String.format("Selected: %s (Score: %.2f)", result.getServer().getName(), result.getTotalScore()));
        printStep("Reason", result.getSelectionReason());

        assertThat(result.getServer().getName()).isEqualTo("NL-Amsterdam-1");
        assertThat(result.getScoreBreakdown().getLoadScore()).isEqualTo(85.0);
        ServerSelectionResult badServerResult = selectionService.calculateServerScore(fiServer, "DE");
        assertThat(badServerResult.getScoreBreakdown().getLoadScore()).isEqualTo(1.5);

        printResult("SUCCESS: Algorithm correctly prioritized low latency and low load.");
    }

    @Test
    @DisplayName("Выбрасывать исключение, если все серверы переполнены или отключены")
    void shouldThrowIfNoServersAvailable() {
        printHeader("TEST: NO AVAILABLE SERVERS FALLBACK");

        ServerDto fullServer = ServerDto.builder()
                .id(1).name("Full-Server")
                .isActive(true).maxConnections(100).currentConnections(100)
                .build();

        when(client.getActiveServers()).thenReturn(List.of(fullServer));

        printStep("Pool", "1 server available, but it is 100% full");

        assertThatThrownBy(() -> selectionService.selectBestServer(new ServerSelectionRequest()))
                .isInstanceOf(NoAvailableServerException.class)
                .hasMessageContaining("All servers are currently full or inactive");

        printResult("SUCCESS: Exception thrown correctly to protect infrastructure.");
    }

    private void printHeader(String title) {
        System.out.println("\n╔" + "═".repeat(78) + "╗");
        System.out.printf("║ %-76s ║\n", title);
        System.out.println("╚" + "═".repeat(78) + "╝");
    }
    private void printStep(String step, String details) {
        System.out.printf("  %-10s │ %s\n", step, details);
    }
    private void printResult(String message) {
        System.out.println("  " + "─".repeat(76));
        System.out.println(message + "\n");
    }
}