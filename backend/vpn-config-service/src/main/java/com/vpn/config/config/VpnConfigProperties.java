package com.vpn.config.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;


@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "vpn.config")
public class VpnConfigProperties {

    private VlessProperties vless = new VlessProperties();
    private QrCodeProperties qrCode = new QrCodeProperties();
    private SelectionProperties selection = new SelectionProperties();

    @Data
    public static class VlessProperties {
        @NotEmpty
        private String defaultSni = "www.google.com";

        @NotEmpty
        private String defaultFingerprint = "chrome";

        @NotEmpty
        private String flow = "xtls-rprx-vision";

        @NotEmpty
        private String networkType = "tcp";
    }

    @Data
    public static class QrCodeProperties {
        private boolean enabled = true;

        @Min(128)
        @Max(2048)
        private int size = 512;

        @NotEmpty
        private String format = "PNG";
    }

    @Data
    public static class SelectionProperties {
        private WeightsProperties weights = new WeightsProperties();
    }

    @Data
    public static class WeightsProperties {
        private double latency = 0.30;
        private double load = 0.20;
        private double geography = 0.25;
        private double health = 0.15;
        private double protocol = 0.10;
    }
}
