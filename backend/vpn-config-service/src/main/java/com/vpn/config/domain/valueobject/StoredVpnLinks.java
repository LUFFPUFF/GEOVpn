package com.vpn.config.domain.valueobject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

public final class StoredVpnLinks {

    private StoredVpnLinks() {}

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DirectLink {

        private Integer serverId;

        private String serverName;

        private String countryCode;

        private String link;

        private Integer avgLatencyMs;

        private Double healthScore;

        private String displayName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RelayLink {

        private Integer serverId;

        private String serverName;

        private String countryCode;

        private String link;

        private Integer relayPriority;

        private String description;
    }

    public static List<DirectLink> emptyDirect() {
        return new ArrayList<>();
    }

    public static List<RelayLink> emptyRelay() {
        return new ArrayList<>();
    }
}
