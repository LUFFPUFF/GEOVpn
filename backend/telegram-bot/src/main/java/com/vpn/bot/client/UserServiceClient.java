package com.vpn.bot.client;

import com.vpn.bot.config.FeignConfig;
import com.vpn.common.dto.ApiResponse;
import com.vpn.common.dto.request.UserRegistrationRequest;
import com.vpn.common.dto.response.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "user-service", url = "${services.user-service.url}", configuration = FeignConfig.class)
public interface UserServiceClient {

    @PostMapping("/api/v1/users/register")
    ApiResponse<UserResponse> registerUser(@RequestBody UserRegistrationRequest request);

    @GetMapping("/api/v1/users/me")
    ApiResponse<UserResponse> getMyProfile(@RequestHeader("X-User-Id") Long telegramId);

    @PostMapping("/api/v1/users/me/subscribe")
    ApiResponse<UserResponse> purchaseSubscription(
            @RequestHeader("X-User-Id") Long telegramId,
            @RequestParam("months") int months
    );

}