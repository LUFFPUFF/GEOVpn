package com.vpn.bot.dto;
import lombok.Data;

@Data
public class VpnConfigResponse {
    private String vlessLink;
    private String qrCodeDataUrl;
    private String serverName;
    private String status;
}