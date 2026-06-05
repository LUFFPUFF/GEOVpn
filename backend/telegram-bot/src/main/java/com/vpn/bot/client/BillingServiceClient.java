package com.vpn.bot.client;

import com.vpn.common.dto.ApiResponse;
import com.vpn.common.dto.request.DepositRequest;
import com.vpn.common.dto.response.DepositResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "billing-service", url = "${services.billing-service.url}")
public interface BillingServiceClient {

    @PostMapping("/api/v1/billing/deposit")
    ApiResponse<DepositResponse> createDeposit(
            @RequestHeader("X-User-Id") long userId,
            @RequestBody DepositRequest request
    );
}
