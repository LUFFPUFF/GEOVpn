package com.vpn.bot.client;

import com.vpn.common.dto.ApiResponse;
import com.vpn.common.dto.request.DeviceCreateRequest;
import com.vpn.common.dto.request.UserRegistrationRequest;
import com.vpn.common.dto.response.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "user-service", url = "${services.user-service.url}")
public interface UserServiceClient {

    @GetMapping("/api/v1/users/me")
    ApiResponse<UserResponse> getMyProfile(@RequestHeader("X-User-Id") long telegramId);

    @GetMapping("/api/v1/users/me/stats")
    ApiResponse<UserStatsResponse> getUserStats(@RequestHeader("X-User-Id") long telegramId);

    @GetMapping("/api/v1/users/leaderboard")
    ApiResponse<List<LeaderboardEntryDto>> getLeaderboard(@RequestHeader("X-User-Id") long telegramId);

    @PostMapping("/api/v1/users/me/apply-promo")
    ApiResponse<UserResponse> applyPromo(@RequestHeader("X-User-Id") long telegramId, @RequestParam("code") String code);

    @PostMapping("/api/v1/users/me/purchase-slot")
    ApiResponse<UserResponse> purchaseExtraSlot(@RequestHeader("X-User-Id") long telegramId);

    @PutMapping("/api/v1/users/me/referral-code")
    ApiResponse<UserResponse> updateReferralCode(@RequestHeader("X-User-Id") long telegramId, @RequestParam("code") String newCode);

    @PostMapping("/api/v1/users/me/subscribe")
    ApiResponse<UserResponse> purchaseSubscription(
            @RequestHeader("X-User-Id") long telegramId,
            @RequestParam("plan") String planName,
            @RequestParam(value = "months", defaultValue = "1") int months,
            @RequestParam(value = "promo", defaultValue = "false") boolean promo
    );

    @GetMapping("/api/v1/devices")
    ApiResponse<List<DeviceResponse>> getMyDevices(@RequestHeader("X-User-Id") long telegramId);

    @PostMapping("/api/v1/users/register")
    ApiResponse<UserResponse> registerUser(@RequestBody UserRegistrationRequest request);

    @GetMapping("/api/v1/users/{telegramId}")
    ApiResponse<UserResponse> getUserByTelegramId(@RequestHeader("X-Internal-Secret") String secret, @PathVariable("telegramId") long telegramId);

    @PostMapping("/api/v1/users/internal/{telegramId}/membership")
    ApiResponse<Void> updateMembership(@PathVariable("telegramId") long telegramId, @RequestParam("isMember") boolean isMember);

    @PostMapping("/api/v1/devices")
    ApiResponse<DeviceResponse> registerDevice(@RequestHeader("X-User-Id") long telegramId, @RequestBody DeviceCreateRequest request);

    @GetMapping("/api/v1/users/me/membership")
    ApiResponse<Boolean> checkMembership(@RequestHeader("X-User-Id") long telegramId);
}