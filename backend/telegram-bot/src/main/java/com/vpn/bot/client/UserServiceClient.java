package com.vpn.bot.client;

import com.vpn.common.dto.ApiResponse;
import com.vpn.common.dto.request.DeviceCreateRequest;
import com.vpn.common.dto.request.UserRegistrationRequest;
import com.vpn.common.dto.response.LeaderboardEntryDto;
import com.vpn.common.dto.response.UserResponse;
import com.vpn.common.dto.response.UserStatsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "user-service", url = "${services.user-service.url}")
public interface UserServiceClient {

    @GetMapping("/api/users/profile/{chatId}")
    ApiResponse<UserResponse> getMyProfile(@PathVariable("chatId") long chatId);

    @GetMapping("/api/users/by-tg-id")
    ApiResponse<UserResponse> getUserByTelegramId(@RequestHeader("X-Internal-Secret") String secret, @RequestParam("tgId") long tgId);

    @PostMapping("/api/users/register")
    ApiResponse<UserResponse> registerUser(@RequestBody UserRegistrationRequest request);

    @PostMapping("/api/users/device")
    ApiResponse<Void> registerDevice(@RequestHeader("X-Internal-Secret") String secret, @RequestParam("tgId") long tgId, @RequestBody DeviceCreateRequest request);

    @PostMapping("/api/users/membership")
    ApiResponse<Void> updateMembership(@RequestParam("chatId") long chatId, @RequestParam("isMember") boolean isMember);

    @GetMapping("/api/users/leaderboard")
    ApiResponse<List<LeaderboardEntryDto>> getLeaderboard(@RequestParam("chatId") long chatId);

    @GetMapping("/api/users/stats/{chatId}")
    ApiResponse<UserStatsResponse> getUserStats(@PathVariable("chatId") long chatId);

    @PostMapping("/api/users/promo/apply")
    ApiResponse<UserResponse> applyPromo(@RequestParam("chatId") long chatId, @RequestParam("code") String code);

    @PostMapping("/api/users/extra-slot")
    ApiResponse<UserResponse> purchaseExtraSlot(@RequestParam("chatId") long chatId);

    @PostMapping("/api/users/referral-code")
    ApiResponse<UserResponse> updateReferralCode(@RequestParam("chatId") long chatId, @RequestParam("newCode") String newCode);
}