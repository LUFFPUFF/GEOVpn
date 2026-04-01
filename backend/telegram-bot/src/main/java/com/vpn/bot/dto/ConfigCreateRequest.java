package com.vpn.bot.dto;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConfigCreateRequest {
    private Long userId;
    private Long deviceId;
    private String protocol;
}