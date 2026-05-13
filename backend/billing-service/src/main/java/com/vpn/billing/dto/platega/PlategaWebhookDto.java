package com.vpn.billing.dto.platega;

import lombok.Data;

@Data
public class PlategaWebhookDto {
    private String transactionId;
    private String status;
    private String payload;
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    private Object additionalData;
}
