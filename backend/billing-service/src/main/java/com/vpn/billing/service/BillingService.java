package com.vpn.billing.service;

import com.vpn.billing.domain.entity.Transaction;
import com.vpn.billing.dto.request.DepositRequest;
import com.vpn.billing.dto.response.DepositResponse;
import com.vpn.billing.dto.response.RevenueStatDto;

import java.util.List;

public interface BillingService {

    DepositResponse createDeposit(Long telegramId, DepositRequest request);

    List<Transaction> getUserTransactions(Long userId);

    List<RevenueStatDto> getRevenueStats(int days);
}
