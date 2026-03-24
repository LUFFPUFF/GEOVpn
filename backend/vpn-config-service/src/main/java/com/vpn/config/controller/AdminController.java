package com.vpn.config.controller;

import com.vpn.common.dto.ApiResponse;
import com.vpn.common.dto.response.VpnConfigResponse;
import com.vpn.common.security.UserRole;
import com.vpn.common.security.annotations.RequireAnyRole;
import com.vpn.config.service.interf.VpnConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final VpnConfigService vpnConfigService;

    @GetMapping("/devices/{deviceId}/config")
    @RequireAnyRole({UserRole.ADMIN})
    public ResponseEntity<ApiResponse<VpnConfigResponse>> getDeviceConfig(@PathVariable Long deviceId) {
        VpnConfigResponse response = vpnConfigService.getConfigByDeviceId(deviceId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

}
