package org.geovpn.bot.client;

import org.geovpn.bot.config.FeignConfig;
import org.geovpn.bot.dto.ApiResponse;
import org.geovpn.bot.dto.ConfigCreateRequest;
import org.geovpn.bot.dto.VpnConfigResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(name = "vpn-config-service", url = "${services.vpn-config-service.url}", configuration = FeignConfig.class)
public interface VpnServiceClient {

    @PostMapping("/api/v1/configs")
    ApiResponse<VpnConfigResponse> createConfig(
            @RequestHeader("X-User-Id") Long telegramId,
            @RequestBody ConfigCreateRequest request
    );

    @GetMapping("/api/v1/configs")
    ApiResponse<List<VpnConfigResponse>> getMyConfigs(
            @RequestHeader("X-User-Id") Long telegramId
    );
}