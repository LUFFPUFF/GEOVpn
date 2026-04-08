package com.vpn.user.grpc;

import com.vpn.common.config.FeignClientConfiguration;
import com.vpn.common.dto.ApiResponse;
import com.vpn.common.dto.request.ConfigCreateRequest;
import com.vpn.common.dto.response.VpnConfigResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "vpn-config-service", url = "http://localhost:8083", configuration = FeignClientConfiguration.class)
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

    @PostMapping("/api/v1/admin/device-limits/{userId}")
    ApiResponse<Object> setDeviceLimit(
            @PathVariable("userId") Long userId,
            @RequestBody Map<String, Object> request
    );
}
