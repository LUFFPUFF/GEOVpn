package com.vpn.billing.client;

import com.vpn.common.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "user-service", url = "${USER_SERVICE_URL}", configuration = FeignClientConfiguration.class)
public interface UserServiceClient {

    @PostMapping("/api/v1/users/{telegramId}/add-balance")
    void addBalance(
            @PathVariable("telegramId") Long telegramId,
            @RequestParam("amount") Integer amount
    );
}
