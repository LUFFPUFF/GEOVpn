package com.vpn.billing.controller;

import com.vpn.billing.domain.entity.Transaction;
import com.vpn.billing.service.BillingService;
import com.vpn.common.dto.ApiResponse;
import com.vpn.common.dto.request.DepositRequest;
import com.vpn.common.dto.response.DepositResponse;
import com.vpn.common.security.annotations.RequireUser;
import com.vpn.common.security.context.SecurityContextHolder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    @PostMapping("/deposit")
    @RequireUser
    public ResponseEntity<ApiResponse<DepositResponse>> createDeposit(
            @Valid @RequestBody DepositRequest request) {

        Long telegramId = SecurityContextHolder.getUserId();
        log.info("Получен запрос на пополнение от пользователя {}: {} RUB", telegramId, request.getAmount());

        DepositResponse response = billingService.createDeposit(telegramId, request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/admin/revenue-stats")
    @com.vpn.common.security.annotations.RequireAnyRole({com.vpn.common.security.UserRole.ADMIN})
    public ResponseEntity<ApiResponse<List<com.vpn.billing.dto.response.RevenueStatDto>>> getRevenueStats(
            @RequestParam(value = "days", defaultValue = "7") int days) {

        return ResponseEntity.ok(ApiResponse.success(billingService.getRevenueStats(days)));
    }

    @GetMapping("/admin/transactions/{userId}")
    @com.vpn.common.security.annotations.RequireAnyRole({com.vpn.common.security.UserRole.ADMIN})
    public ResponseEntity<ApiResponse<List<Transaction>>> getUserTransactionsForAdmin(
            @PathVariable Long userId) {

        log.info("Admin: Запрос транзакций для пользователя {}", userId);
        List<Transaction> transactions = billingService.getUserTransactions(userId);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

}
