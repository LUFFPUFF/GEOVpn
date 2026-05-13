package com.vpn.billing.dto.platega;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlategaCreateRequest {

    private Integer paymentMethod;
    private PaymentDetails paymentDetails;
    private String description;

    @JsonProperty("return")
    private String returnUrl;

    private String failedUrl;

    private String payload;

    @Data
    @Builder
    public static class PaymentDetails {
        private Double amount;
        private String currency;
    }
}
