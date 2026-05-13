package com.vpn.billing.service;

import com.vpn.billing.dto.platega.PlategaWebhookDto;

public interface WebhookService {
    void processWebhook(String merchantId, String secret, PlategaWebhookDto webhookDto);
}
