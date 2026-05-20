package com.vpn.user.controller;

import com.vpn.common.dto.ApiResponse;
import com.vpn.common.dto.request.UserRegistrationRequest;
import com.vpn.common.dto.response.LeaderboardEntryDto;
import com.vpn.common.dto.response.TrafficStatsResponse;
import com.vpn.common.dto.response.UserResponse;
import com.vpn.common.dto.response.UserStatsResponse;
import com.vpn.common.dto.request.UserUpdateRequest;
import com.vpn.common.exception.UnauthorizedException;
import com.vpn.common.security.UserRole;
import com.vpn.common.security.annotations.*;
import com.vpn.common.security.context.SecurityContextHolder;
import com.vpn.user.service.interf.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    @Public
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody UserRegistrationRequest request) {
        UserResponse response = userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/{telegramId}")
    @RequireAnyRole({UserRole.ADMIN, UserRole.SERVICE})
    public ResponseEntity<ApiResponse<UserResponse>> getUserByTelegramId(@PathVariable Long telegramId) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserByTelegramId(telegramId)));
    }

    @PostMapping("/me/apply-promo")
    @RequireUser
    public ResponseEntity<ApiResponse<UserResponse>> applyPromoCode(
            @RequestParam("code") String code) {
        Long telegramId = SecurityContextHolder.getUserId();
        UserResponse response = userService.applyPromoCode(telegramId, code);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/me/referral-code")
    @RequireUser
    public ResponseEntity<ApiResponse<UserResponse>> updateReferralCode(
            @RequestParam("code") String newCode) {
        Long telegramId = SecurityContextHolder.getUserId();
        UserResponse response = userService.updateReferralCode(telegramId, newCode.toUpperCase());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/me/easter-egg")
    @RequireUser
    public ResponseEntity<ApiResponse<UserResponse>> claimEasterEgg() {
        Long telegramId = SecurityContextHolder.getUserId();
        UserResponse response = userService.claimEasterEgg(telegramId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/me/purchase-slot")
    @RequireUser
    public ResponseEntity<ApiResponse<UserResponse>> purchaseSlot() {
        Long telegramId = SecurityContextHolder.getUserId();
        UserResponse response = userService.purchaseExtraSlot(telegramId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/me/membership")
    @RequireUser
    public ResponseEntity<ApiResponse<Boolean>> checkMembership() {
        Long telegramId = SecurityContextHolder.getUserId();
        return ResponseEntity.ok(ApiResponse.success(userService.isUserMemberOfChannel(telegramId)));
    }

    /**
     * Оформление подписки.
     *
     * Обычная: POST /api/v1/users/me/subscribe?plan=BASIC&months=1
     * Промо:   POST /api/v1/users/me/subscribe?plan=BASIC&months=1&promo=true
     *
     * Промо выдаётся только пользователям с подпиской PAYG (не платили ранее).
     * Повторная попытка вернёт текущий профиль без изменений.
     */
    @PostMapping("/me/subscribe")
    @RequireUser
    public ResponseEntity<ApiResponse<UserResponse>> subscribe(
            @RequestParam("plan") String planName,
            @RequestParam(value = "months", defaultValue = "1") int months,
            @RequestParam(value = "promo", defaultValue = "false") boolean promo) {

        Long telegramId = SecurityContextHolder.getUserId();
        UserResponse response = userService.purchaseSubscription(telegramId, planName, months, promo);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/leaderboard")
    @RequireUser
    public ResponseEntity<ApiResponse<List<LeaderboardEntryDto>>> getLeaderboard() {
        return ResponseEntity.ok(ApiResponse.success(userService.getLeaderboard()));
    }

    @GetMapping("/me")
    @RequireUser
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile() {
        Long telegramId = SecurityContextHolder.getUserId();
        return ResponseEntity.ok(ApiResponse.success(userService.getUserByTelegramId(telegramId)));
    }

    @PutMapping("/me")
    @RequireUser
    public ResponseEntity<ApiResponse<UserResponse>> updateMyProfile(
            @RequestHeader("X-User-Id") Long telegramId,
            @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.updateUser(telegramId, request)));
    }

    @GetMapping("/me/stats")
    @RequireUser
    public ResponseEntity<ApiResponse<UserStatsResponse>> getMyStats() {
        Long telegramId = SecurityContextHolder.getUserId();
        return ResponseEntity.ok(ApiResponse.success(userService.getUserStats(telegramId)));
    }

    @DeleteMapping("/me")
    @RequireUser
    public ResponseEntity<ApiResponse<Void>> deactivateMyAccount(
            @RequestHeader("X-User-Id") Long telegramId) {
        userService.deactivateUser(telegramId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping
    @RequireAdmin
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(userService.getAllUsers(page, size)));
    }

    @PostMapping("/{telegramId}/deduct-balance")
    @RequireService
    public ResponseEntity<ApiResponse<Integer>> deductBalance(
            @PathVariable Long telegramId,
            @RequestParam Integer amount) {
        var updatedUser = userService.deductBalance(telegramId, amount);
        return ResponseEntity.ok(ApiResponse.success(updatedUser.getBalance()));
    }

    @GetMapping("/me/traffic")
    @RequireUser
    public ResponseEntity<ApiResponse<TrafficStatsResponse>> getMyTrafficStats() {
        Long telegramId = SecurityContextHolder.getUserId();
        return ResponseEntity.ok(ApiResponse.success(userService.getTrafficStats(telegramId)));
    }

    @GetMapping("/{telegramId}/traffic")
    @RequireAnyRole({UserRole.ADMIN, UserRole.SERVICE})
    public ResponseEntity<ApiResponse<TrafficStatsResponse>> getUserTrafficStats(
            @PathVariable Long telegramId) {
        return ResponseEntity.ok(ApiResponse.success(userService.getTrafficStats(telegramId)));
    }
}