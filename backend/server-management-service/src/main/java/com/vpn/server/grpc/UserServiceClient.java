package com.vpn.server.grpc;

import com.vpn.common.dto.ApiResponse;
import com.vpn.common.config.FeignClientConfiguration;
import com.vpn.common.dto.request.DeviceCreateRequest;
import com.vpn.common.dto.request.UserRegistrationRequest;
import com.vpn.common.dto.response.DeviceResponse;
import com.vpn.common.dto.response.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "user-service", url = "http://localhost:8082")
public interface UserServiceClient {
    @PostMapping("/api/v1/devices")
    ApiResponse<DeviceResponse> registerDevice(@RequestHeader("X-User-Id") Long userId, @RequestBody DeviceCreateRequest request);

    @PostMapping("/api/v1/users/{telegramId}/deduct-balance")
    ApiResponse<Integer> deductBalance(@PathVariable Long telegramId, @RequestParam Integer amount);

    @PostMapping("/api/v1/users/register")
    ApiResponse<UserResponse> registerUser(@RequestBody UserRegistrationRequest request);

    @GetMapping("/api/v1/users/{telegramId}")
    ApiResponse<UserResponse> getUserByTelegramId(@PathVariable Long telegramId);
}
