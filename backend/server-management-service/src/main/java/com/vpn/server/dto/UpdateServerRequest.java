package com.vpn.server.dto;

import lombok.Data;

@Data
public class UpdateServerRequest {
    private String location;
    private Integer maxConnections;
    private Boolean isActive;

    private String ipAddress;
    private Integer port;
    private String realitySni;
    private String realityPublicKey;
    private String realityShortId;

    private String relaySni;
    private String relayPublicKey;
    private String relayShortId;
}
