package com.vpn.config.generator;

import com.vpn.config.config.VpnConfigProperties;
import com.vpn.config.domain.valueobject.ServerAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class VpnGeneratorFlowTest {

    private UuidGenerator uuidGenerator;
    private RealityConfigGenerator realityGenerator;

    @Mock
    private VpnConfigProperties configProperties;

    private VlessLinkBuilder vlessLinkBuilder;
    private QRCodeGenerator qrCodeGenerator;

    @BeforeEach
    void setUp() {
        uuidGenerator = new UuidGenerator();
        realityGenerator = new RealityConfigGenerator();

        VpnConfigProperties.VlessProperties vless = new VpnConfigProperties.VlessProperties();
        vless.setDefaultSni("www.microsoft.com");
        vless.setDefaultFingerprint("chrome");
        vless.setNetworkType("tcp");
        vless.setFlow("xtls-rprx-vision");

        lenient().when(configProperties.getVless()).thenReturn(vless);

        VpnConfigProperties.QrCodeProperties qr = new VpnConfigProperties.QrCodeProperties();
        qr.setEnabled(true);
        qr.setSize(300);
        qr.setFormat("PNG");
        lenient().when(configProperties.getQrCode()).thenReturn(qr);

        vlessLinkBuilder = new VlessLinkBuilder(configProperties);
        qrCodeGenerator = new QRCodeGenerator(configProperties);
    }

    @Test
    @DisplayName("Полная генерация конфигурации VPN и экспорт файлов")
    void fullGenerationFlow() throws Exception {
        printHeader("STARTING VPN CONFIG GENERATION TEST");

        UUID deviceUuid = uuidGenerator.generateDeterministicUuid(12345L, 1L);
        printStep(1, "UUID Generation", deviceUuid.toString());

        String publicKey = "hCbGtUqtNFQOJcbg-t6OvBmd1ZZZy819ZHEOGTHOJFk";
        String shortId = "c039559afc623939";
        printStep(2, "Reality Params", "PBK: " + publicKey + " | SID: " + shortId);

        ServerAddress address = new ServerAddress("193.104.33.209");
        String vlessLink = vlessLinkBuilder.buildVlessLink(
                deviceUuid, deviceUuid.toString(),  address, 443, "GeoVpn", publicKey, shortId);
        printStep(3, "VLESS URL", vlessLink);

        String qrBase64 = qrCodeGenerator.generateQRCodeBase64(vlessLink);
        printStep(4, "QR Base64 length", String.valueOf(qrBase64.length()));

        Path outputPath = Paths.get("target", "test-outputs");
        Files.createDirectories(outputPath);
        String fileName = "test-vpn-qr.png";
        Path filePath = outputPath.resolve(fileName);

        qrCodeGenerator.saveQRCodeToFile(vlessLink, filePath.toString());
        printStep(5, "QR Code Export", "SUCCESS saved to " + filePath.toAbsolutePath());

        assertThat(new File(filePath.toString())).exists();
        assertThat(vlessLink).startsWith("vless://");
    }

    @Test
    @DisplayName("Детерминированный тест UUID")
    void testDeterministicUuid() {
        printHeader("TESTING DETERMINISTIC UUID");

        UUID uuid1 = uuidGenerator.generateDeterministicUuid(777L, 99L);
        UUID uuid2 = uuidGenerator.generateDeterministicUuid(777L, 99L);

        printStep(1, "Attempt 1", uuid1.toString());
        printStep(2, "Attempt 2", uuid2.toString());

        assertThat(uuid1).isEqualTo(uuid2);
    }

    private void printHeader(String title) {
        System.out.println("\n" + "╔" + "═".repeat(78) + "╗");
        System.out.printf("║ %-76s ║\n", title);
        System.out.println("╚" + "═".repeat(78) + "╝");
    }

    private void printStep(int step, String name, String value) {
        System.out.printf("  %d. %-22s │ %s\n", step, name, value);
    }
}