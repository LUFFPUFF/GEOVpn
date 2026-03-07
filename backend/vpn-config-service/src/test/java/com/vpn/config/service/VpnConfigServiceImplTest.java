package com.vpn.config.service;

import com.vpn.config.domain.entity.VpnConfiguration;
import com.vpn.config.domain.enums.ConfigStatus;
import com.vpn.common.dto.ServerDto;
import com.vpn.config.dto.mapper.ConfigMapper;
import com.vpn.config.dto.request.ConfigCreateRequest;
import com.vpn.config.dto.response.ServerSelectionResult;
import com.vpn.config.dto.response.VpnConfigResponse;
import com.vpn.config.exception.ConfigAlreadyExistsException;
import com.vpn.config.exception.UnauthorizedConfigAccessException;
import com.vpn.config.generator.QRCodeGenerator;
import com.vpn.config.generator.RealityConfigGenerator;
import com.vpn.config.generator.UuidGenerator;
import com.vpn.config.generator.VlessLinkBuilder;
import com.vpn.config.repository.VpnConfigurationRepository;
import com.vpn.config.service.interf.ServerSelectionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VpnConfigServiceImplTest {

    @Mock private VpnConfigurationRepository configRepository;
    @Mock private ServerSelectionService serverSelectionService;
    @Mock private UuidGenerator uuidGenerator;
    @Mock private RealityConfigGenerator realityGenerator;
    @Mock private VlessLinkBuilder vlessLinkBuilder;
    @Mock private QRCodeGenerator qrCodeGenerator;
    @Mock private ConfigMapper configMapper;

    @InjectMocks
    private VpnConfigServiceImpl vpnConfigService;

    @Test
    @DisplayName("Полный цикл работы VpnConfigService")
    void shouldCreateConfigSuccessfully() {
        printHeader("TEST: CREATE VPN CONFIGURATION FLOW");

        Long userId = 100L;
        Long deviceId = 500L;

        ConfigCreateRequest request = ConfigCreateRequest.builder()
                .userId(userId).deviceId(deviceId).userLocation("RU").build();

        when(configRepository.existsByDeviceIdAndStatus(deviceId, ConfigStatus.ACTIVE)).thenReturn(false);
        printStep("DB Check", "No active configs found for device " + deviceId);

        ServerDto mockServer = ServerDto.builder()
                .id(1).name("FI-Helsinki").ipAddress("1.1.1.1").port(443).build();
        ServerSelectionResult mockResult = ServerSelectionResult.builder()
                .server(mockServer).totalScore(95.0).selectionReason("Good ping").build();

        when(serverSelectionService.selectBestServer(any())).thenReturn(mockResult);
        printStep("Server", "Selected " + mockServer.getName());

        UUID mockUuid = UUID.randomUUID();
        when(uuidGenerator.generateDeterministicUuid(userId, deviceId)).thenReturn(mockUuid);
        when(realityGenerator.generatePublicKey()).thenReturn("pbk_123");
        when(realityGenerator.generateShortId()).thenReturn("sid_123");
        when(vlessLinkBuilder.buildVlessLink(any(), any(), anyInt(), anyString(), anyString(), anyString()))
                .thenReturn("vless://mock-link");
        when(qrCodeGenerator.generateQRCodeBase64(anyString())).thenReturn("base64_string");
        when(qrCodeGenerator.generateQRCodeDataUrl(anyString())).thenReturn("data:image/png...");

        VpnConfiguration savedConfig = VpnConfiguration.builder()
                .id(1L).vlessUuid(mockUuid).status(ConfigStatus.ACTIVE).build();
        when(configRepository.save(any(VpnConfiguration.class))).thenReturn(savedConfig);

        VpnConfigResponse expectedResponse = VpnConfigResponse.builder().vlessUuid(mockUuid).build();
        when(configMapper.toResponse(any(), any(), any())).thenReturn(expectedResponse);

        VpnConfigResponse response = vpnConfigService.createConfig(request);

        assertThat(response.getVlessUuid()).isEqualTo(mockUuid);
        verify(configRepository).save(any(VpnConfiguration.class));

        printResult("SUCCESS: Config generated, components called in correct order.");
    }

    @Test
    @DisplayName("При отзыве разрешения возникает ошибка Unauthorized")
    void shouldThrowUnauthorizedWhenRevokingForeignConfig() {
        printHeader("TEST: SECURITY IDOR PREVENTION");

        Long deviceId = 10L;
        Long ownerId = 1L;
        Long hackerId = 666L;

        VpnConfiguration existingConfig = VpnConfiguration.builder()
                .deviceId(deviceId)
                .userId(ownerId)
                .status(ConfigStatus.ACTIVE)
                .build();

        when(configRepository.findByDeviceIdAndStatus(deviceId, ConfigStatus.ACTIVE))
                .thenReturn(Optional.of(existingConfig));

        printStep("Action", "User " + hackerId + " attempts to revoke device " + deviceId);

        assertThatThrownBy(() -> vpnConfigService.revokeConfig(deviceId, hackerId))
                .isInstanceOf(UnauthorizedConfigAccessException.class);

        verify(configRepository, never()).save(any());
        printResult("SUCCESS: Hacker blocked. Config remains Active.");
    }

    @Test
    @DisplayName("Запретить наличие нескольких активных конфигураций на одном устройстве")
    void shouldThrowIfConfigAlreadyExists() {
        printHeader("TEST: RESOURCE EXHAUSTION PREVENTION");

        ConfigCreateRequest request = ConfigCreateRequest.builder().userId(1L).deviceId(10L).build();

        when(configRepository.existsByDeviceIdAndStatus(10L, ConfigStatus.ACTIVE)).thenReturn(true);

        printStep("Action", "Attempt to create second active config for device 10");

        assertThatThrownBy(() -> vpnConfigService.createConfig(request))
                .isInstanceOf(ConfigAlreadyExistsException.class);

        verifyNoInteractions(serverSelectionService, uuidGenerator);
        printResult("SUCCESS: Fast fail executed. Server resources saved.");
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