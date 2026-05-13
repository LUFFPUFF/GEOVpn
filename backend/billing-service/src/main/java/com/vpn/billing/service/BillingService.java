package com.vpn.billing.service;

import com.vpn.billing.dto.request.DepositRequest;
import com.vpn.billing.dto.response.DepositResponse;

public interface BillingService {

    DepositResponse createDeposit(Long telegramId, DepositRequest request);
}
