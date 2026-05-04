package com.vpn.server.grpc;

import com.vpn.common.dto.ApiResponse;
import com.vpn.common.config.FeignClientConfiguration;
import com.vpn.common.dto.request.ConfigCreateRequest;
import com.vpn.common.dto.response.VpnConfigResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "vpn-config-service", url = "${VPN_CONFIG_SERVICE_URL}", configuration = FeignClientConfiguration.class)
public interface ConfigServiceClient {
    @PostMapping("/api/v1/configs")
    ApiResponse<VpnConfigResponse> createConfig(@RequestHeader("X-User-Id") Long telegramId, @RequestBody ConfigCreateRequest request);
}
