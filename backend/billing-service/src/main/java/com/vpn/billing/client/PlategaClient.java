package com.vpn.billing.client;

import com.vpn.billing.dto.platega.PlategaCreateRequest;
import com.vpn.billing.dto.platega.PlategaCreateResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "platega-client", url = "https://app.platega.io")
public interface PlategaClient {

    @PostMapping("/transaction/process")
    PlategaCreateResponse createTransaction(
            @RequestHeader("X-MerchantId") String merchantId,
            @RequestHeader("X-Secret") String secret,
            @RequestBody PlategaCreateRequest request
    );

    @PostMapping("/v2/transaction/process")
    PlategaCreateResponse createTransactionV2(
            @RequestHeader("X-MerchantId") String merchantId,
            @RequestHeader("X-Secret") String secret,
            @RequestBody PlategaCreateRequest request
    );

}
