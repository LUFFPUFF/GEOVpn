package com.vpn.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateServerRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Size(max = 50)
    private String location;

    @NotBlank
    @Size(min = 2, max = 2)
    @Pattern(regexp = "[A-Z]{2}")
    private String countryCode;

    @NotBlank
    @Pattern(regexp = "^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$")
    private String ipAddress;

    @NotNull
    private Integer port = 443;

    private Integer grpcPort = 10085;

    @NotBlank
    private String realityPublicKey;

    @NotBlank
    @Size(max = 50)
    private String realityShortId;

    private String realitySni = "www.microsoft.com";

    private Integer maxConnections = 1000;
}
