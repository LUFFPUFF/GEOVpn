package com.vpn.server.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UpdateServerRequest {
    @Size(max = 50)
    private String location;
    @Min(0)
    @Max(10000)
    private Integer maxConnections;
    private Boolean isActive;
    @Size(max = 255)
    private String realitySni;
    @Min(0)
    @Max(100)
    private Double healthScore;
    @Size(max = 500)
    private String adminNotes;
}
