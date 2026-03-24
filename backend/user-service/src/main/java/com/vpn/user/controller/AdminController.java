package com.vpn.user.controller;

import com.vpn.common.dto.ApiResponse;
import com.vpn.common.dto.response.AdminDashboardResponse;
import com.vpn.common.dto.response.DeviceResponse;
import com.vpn.common.dto.response.UserResponse;
import com.vpn.common.dto.response.UserStatsResponse;
import com.vpn.common.security.UserRole;
import com.vpn.common.security.annotations.RequireAnyRole;
import com.vpn.user.service.interf.AdminService;
import com.vpn.user.service.interf.DeviceService;
import com.vpn.user.service.interf.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final DeviceService deviceService;
    private final UserService userService;

    /**
     * Получить главные метрики для Дашборда (React Panel)
     */
    @GetMapping("/dashboard")
    @RequireAnyRole({UserRole.ADMIN})
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getDashboardStats() {
        AdminDashboardResponse stats = adminService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * Получить список всех пользователей (с пагинацией для таблицы в React)
     */
    @GetMapping("/users")
    @RequireAnyRole({UserRole.ADMIN})
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size) {

        List<UserResponse> users = userService.getAllUsers(page, size);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    /**
     * Ручное пополнение баланса администратором (например, подарок юзеру)
     */
    @PostMapping("/users/{telegramId}/add-balance")
    @RequireAnyRole({UserRole.ADMIN})
    public ResponseEntity<ApiResponse<UserResponse>> addBalanceToUser(
            @PathVariable Long telegramId,
            @RequestParam Integer amount) {

        UserResponse updatedUser = userService.addBalance(telegramId, amount);
        return ResponseEntity.ok(ApiResponse.success(updatedUser));
    }

    @GetMapping("/users/{telegramId}/devices")
    @RequireAnyRole({UserRole.ADMIN})
    public ResponseEntity<ApiResponse<List<DeviceResponse>>> getUserDevices(@PathVariable Long telegramId) {
        List<DeviceResponse> devices = deviceService.getUserActiveDevices(telegramId);
        return ResponseEntity.ok(ApiResponse.success(devices));
    }

    @GetMapping("/users/{telegramId}/stats")
    @RequireAnyRole({UserRole.ADMIN})
    public ResponseEntity<ApiResponse<UserStatsResponse>> getUserStatsForAdmin(@PathVariable Long telegramId) {
        UserStatsResponse stats = userService.getUserStats(telegramId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
