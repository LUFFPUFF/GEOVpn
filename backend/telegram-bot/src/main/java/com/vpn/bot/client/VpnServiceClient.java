package com.vpn.bot.client;

import com.vpn.common.dto.ApiResponse;
import com.vpn.common.dto.request.ConfigCreateRequest;
import com.vpn.common.dto.response.VpnConfigResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "vpn-service", url = "${services.vpn-service.url}")
public interface VpnServiceClient {

    @PostMapping("/api/v1/configs")
    ApiResponse<VpnConfigResponse> createConfig(
            @RequestHeader("X-User-Id") long telegramId,
            @RequestBody ConfigCreateRequest request
    );

    @GetMapping("/api/v1/configs/configs")
    ApiResponse<List<VpnConfigResponse>> getMyConfigs(
            @RequestHeader("X-User-Id") long telegramId
    );
}