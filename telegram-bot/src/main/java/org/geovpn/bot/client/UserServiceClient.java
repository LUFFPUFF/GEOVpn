package org.geovpn.bot.client;

import org.geovpn.bot.config.FeignConfig;
import org.geovpn.bot.dto.ApiResponse;
import org.geovpn.bot.dto.UserRegistrationRequest;
import org.geovpn.bot.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "user-service", url = "${services.user-service.url}", configuration = FeignConfig.class)
public interface UserServiceClient {

    @PostMapping("/api/v1/users/register")
    ApiResponse<UserResponse> registerUser(@RequestBody UserRegistrationRequest request);

    @GetMapping("/api/v1/users/me")
    ApiResponse<UserResponse> getMyProfile(@RequestHeader("X-User-Id") Long telegramId);

}