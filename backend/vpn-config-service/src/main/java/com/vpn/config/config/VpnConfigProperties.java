package com.vpn.config.config;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

import java.util.HashMap;
import java.util.Map;


@Slf4j
@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "vpn.config")
public class VpnConfigProperties {

    private VlessProperties vless = new VlessProperties();
    private QrCodeProperties qrCode = new QrCodeProperties();
    private SelectionProperties selection = new SelectionProperties();

    @PostConstruct
    public void validate() {
        double totalWeight = selection.getWeights().getLatency() +
                selection.getWeights().getLoad() +
                selection.getWeights().getGeography() +
                selection.getWeights().getHealth() +
                selection.getWeights().getProtocol();

        if (Math.abs(totalWeight - 1.0) > 0.001) {
            log.warn("Weights sum is {}, expected 1.0. Auto-normalizing...", totalWeight);
            normalizeWeights();
        }

        log.info("VPN Config Properties validated successfully");
        log.debug("Weights: Latency={}, Load={}, Geography={}, Health={}, Protocol={}",
                selection.getWeights().getLatency(),
                selection.getWeights().getLoad(),
                selection.getWeights().getGeography(),
                selection.getWeights().getHealth(),
                selection.getWeights().getProtocol());
    }

    private void normalizeWeights() {
        WeightsProperties weights = selection.getWeights();
        double total = weights.getLatency() + weights.getLoad() +
                weights.getGeography() + weights.getHealth() +
                weights.getProtocol();

        weights.setLatency(weights.getLatency() / total);
        weights.setLoad(weights.getLoad() / total);
        weights.setGeography(weights.getGeography() / total);
        weights.setHealth(weights.getHealth() / total);
        weights.setProtocol(weights.getProtocol() / total);

        log.info("Weights normalized to sum=1.0");
    }

    @Data
    public static class VlessProperties {
        @NotEmpty
        private String defaultSni = "mega.nz";

        private String relaySni = "eh.vk.com";

        @NotEmpty
        private String defaultFingerprint = "chrome";

        @NotEmpty
        private String flow = "xtls-rprx-vision";

        @NotEmpty
        private String networkType = "tcp";

        @NotEmpty
        private String relayServerAddress;

        @NotNull
        private Integer relayServerPort = 443;
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
        private Map<String, Map<String, Integer>> geographyBonus = new HashMap<>();
        private LatencyProperties latency = new LatencyProperties();
        private LoadProperties load = new LoadProperties();

        public int getGeographyBonus(String countryCode, String userRegion) {
            if (userRegion != null && geographyBonus.containsKey(userRegion)) {
                Map<String, Integer> regionBonus = geographyBonus.get(userRegion);
                return regionBonus.getOrDefault(countryCode, 0);
            }

            Map<String, Integer> defaultBonus = geographyBonus.get("default");
            if (defaultBonus != null) {
                return defaultBonus.getOrDefault(countryCode, 0);
            }

            return 0;
        }
    }

    @Data
    public static class WeightsProperties {
        @Min(0)
        @Max(1)
        private double latency = 0.30;

        @Min(0)
        @Max(1)
        private double load = 0.20;

        @Min(0)
        @Max(1)
        private double geography = 0.25;

        @Min(0)
        @Max(1)
        private double health = 0.15;

        @Min(0)
        @Max(1)
        private double protocol = 0.10;
    }

    @Data
    public static class LatencyProperties {
        private double multiplier = 0.5;

        @Min(0)
        private int maxAcceptableMs = 200;

        @Min(0)
        private int excellentThreshold = 20;

        @Min(0)
        private int goodThreshold = 50;
    }

    @Data
    public static class LoadProperties {
        @Min(0)
        @Max(100)
        private double criticalThreshold = 90.0;

        @Min(0)
        @Max(100)
        private double warningThreshold = 70.0;

        @Min(0)
        @Max(100)
        private double optimalThreshold = 50.0;
    }
}
