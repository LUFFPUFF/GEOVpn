package com.vpn.billing.controller;

import com.vpn.billing.dto.platega.PlategaWebhookDto;
import com.vpn.billing.service.WebhookService;
import com.vpn.common.security.annotations.Public;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/billing/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping
    public ResponseEntity<String> receiveWebhook(
            @RequestHeader(value = "X-MerchantId", required = false) String merchantId,
            @RequestHeader(value = "X-Secret", required = false) String secret,
            @RequestBody PlategaWebhookDto webhookDto) {

        log.debug("Пришел вебхук, Payload: {}", webhookDto);

        try {
            webhookService.processWebhook(merchantId, secret, webhookDto);
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("Ошибка при обработке вебхука: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Error processing webhook");
        }
    }
}
