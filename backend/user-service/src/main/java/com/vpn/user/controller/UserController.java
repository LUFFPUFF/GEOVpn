package com.vpn.user.controller;

import com.vpn.common.dto.ApiResponse;
import com.vpn.user.dto.response.UserResponse;
import com.vpn.user.dto.response.UserStatsResponse;
import com.vpn.user.dto.response.UserUpdateRequest;
import com.vpn.user.service.interf.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Получить профиль текущего пользователя
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile(
            @RequestHeader("X-User-Id") Long telegramId) {

        UserResponse response = userService.getUserByTelegramId(telegramId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Обновить профиль (имя, юзернейм)
     */
    @PutMapping("/me")
    @PreAuthorize("hasRole('USER')")
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
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<UserStatsResponse>> getMyStats(
            @RequestHeader("X-User-Id") Long telegramId) {

        UserStatsResponse response = userService.getUserStats(telegramId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Деактивировать аккаунт (Soft delete)
     */
    @DeleteMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> deactivateMyAccount(
            @RequestHeader("X-User-Id") Long telegramId) {

        userService.deactivateUser(telegramId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        List<UserResponse> users = userService.getAllUsers(page, size);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @PostMapping("/{telegramId}/deduct")
    @PreAuthorize("hasAnyRole('SERVICE', 'ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> deductBalanceForService(
            @PathVariable("telegramId") Long telegramId,
            @RequestParam("amount") Integer amount) {
        UserResponse response = userService.deductBalance(telegramId, amount);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
