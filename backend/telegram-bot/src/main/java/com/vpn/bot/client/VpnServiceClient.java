package com.vpn.bot.client;

import com.vpn.common.dto.ApiResponse;
import com.vpn.common.dto.request.ConfigCreateRequest;
import com.vpn.common.dto.response.VpnConfigResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "vpn-service", url = "${services.vpn-service.url}")
public interface VpnServiceClient {

    @PostMapping("/api/vpn/configs")
    ApiResponse<VpnConfigResponse> createConfig(@RequestParam("chatId") long chatId, @RequestBody ConfigCreateRequest request);

    @GetMapping("/api/vpn/configs/my")
    ApiResponse<List<VpnConfigResponse>> getMyConfigs(@RequestParam("chatId") long chatId);
}