package com.vpn.config.client;

import com.vpn.common.config.FeignClientConfiguration;
import com.vpn.common.dto.ApiResponse;
import com.vpn.common.dto.request.ConnectionUpdateRequest;
import com.vpn.common.dto.request.DeviceCreateRequest;
import com.vpn.common.dto.request.UserRegistrationRequest;
import com.vpn.common.dto.response.DeviceResponse;
import com.vpn.common.dto.response.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "user-service", url = "${USER_SERVICE_URL}", configuration = FeignClientConfiguration.class)
public interface UserServiceClient {

    @PostMapping("/api/v1/users/register")
    ApiResponse<UserResponse> registerUser(@RequestBody UserRegistrationRequest request);

    @GetMapping("/api/v1/users/{telegramId}")
    ApiResponse<UserResponse> getUserByTelegramId(@PathVariable Long telegramId);

    @PostMapping("/api/v1/users/{telegramId}/deduct-balance")
    ApiResponse<Integer> deductBalance(@PathVariable Long telegramId, @RequestParam Integer amount);

    @PostMapping("/api/v1/devices")
    ApiResponse<DeviceResponse> registerDevice(@RequestHeader("X-User-Id") Long userId, @RequestBody DeviceCreateRequest request);

    @PostMapping("/api/v1/connections/update")
    ApiResponse<Void> updateConnection(@RequestBody ConnectionUpdateRequest request);

    @PostMapping("/api/v1/connections/close")
    ApiResponse<Void> closeConnection(@RequestBody ConnectionUpdateRequest request);

    @DeleteMapping("/api/v1/devices/internal/{deviceId}")
    void deleteDeviceById(
            @PathVariable("deviceId") Long deviceId,
            @RequestHeader("X-User-Id") Long userId
    );

    @PostMapping("/api/v1/users/internal/{telegramId}/ban")
    ApiResponse<Void> updateBanStatus(
            @PathVariable("telegramId") Long telegramId,
            @RequestParam("isBanned") boolean isBanned,
            @RequestParam(value = "reason", required = false) String reason
    );

    @GetMapping("/api/v1/devices/user/{telegramId}/active")
    ApiResponse<List<DeviceResponse>> getUserActiveDevices(@PathVariable("telegramId") Long telegramId);

    @GetMapping("/api/v1/users/internal/active-ids")
    ApiResponse<List<Long>> getAllActiveUserIds();
}
