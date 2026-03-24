package com.vpn.user.controller;

import com.vpn.common.dto.ApiResponse;
import com.vpn.common.dto.request.ConnectionUpdateRequest;
import com.vpn.common.security.annotations.RequireService;
import com.vpn.user.service.ConnectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/connections")
@RequiredArgsConstructor
public class ConnectionController {

    private final ConnectionService connectionService;

    /**
     * Обновить или создать сессию (вызывается из server-service)
     */
    @PostMapping("/update")
    @RequireService
    public ResponseEntity<ApiResponse<Void>> updateConnection(
            @RequestBody ConnectionUpdateRequest request) {

        connectionService.updateConnection(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Закрыть сессию при отключении пользователя
     */
    @PostMapping("/close")
    @RequireService
    public ResponseEntity<ApiResponse<Void>> closeConnection(
            @RequestBody ConnectionUpdateRequest request) {

        connectionService.closeConnection(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
