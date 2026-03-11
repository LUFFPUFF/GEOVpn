package com.vpn.user.controller;

import com.vpn.common.dto.ApiResponse;
import com.vpn.common.dto.request.UserRegistrationRequest;
import com.vpn.common.dto.response.UserResponse;
import com.vpn.common.dto.response.UserStatsResponse;
import com.vpn.common.dto.request.UserUpdateRequest;
import com.vpn.common.security.UserRole;
import com.vpn.common.security.annotations.*;
import com.vpn.user.service.interf.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @GetMapping("/{telegramId}")
    @RequireAnyRole({UserRole.ADMIN, UserRole.SERVICE})
    public ResponseEntity<ApiResponse<UserResponse>> getUserByTelegramId(@PathVariable Long telegramId) {
        UserResponse response = userService.getUserByTelegramId(telegramId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Получить профиль текущего пользователя
     */
    @GetMapping("/me")
    @RequireUser
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile(
            @RequestHeader("X-User-Id") Long telegramId) {

        UserResponse response = userService.getUserByTelegramId(telegramId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Обновить профиль (имя, юзернейм)
     */
    @PutMapping("/me")
    @RequireUser
    public ResponseEntity<ApiResponse<UserResponse>> updateMyProfile(
            @RequestHeader("X-User-Id") Long telegramId,
            @Valid @RequestBody UserUpdateRequest request) {

        UserResponse response = userService.updateUser(telegramId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Получить подробную статистику пользователя (устройства, рефералы, баланс)
     */
    @GetMapping("/me/stats")
    @RequireUser
    public ResponseEntity<ApiResponse<UserStatsResponse>> getMyStats(
            @RequestHeader("X-User-Id") Long telegramId) {

        UserStatsResponse response = userService.getUserStats(telegramId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Деактивировать аккаунт (Soft delete)
     */
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
        List<UserResponse> users = userService.getAllUsers(page, size);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @PostMapping("/{telegramId}/deduct-balance")
    @RequireService
    public ResponseEntity<ApiResponse<Integer>> deductBalance(
            @PathVariable Long telegramId,
            @RequestParam Integer amount) {
        var updatedUser = userService.deductBalance(telegramId, amount);
        return ResponseEntity.ok(ApiResponse.success(updatedUser.getBalance()));
    }
}
