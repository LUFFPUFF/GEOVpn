package com.vpn.billing.dto.platega;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PlategaCreateResponse {
    private String paymentMethod;
    private String transactionId;
    private String redirect;
    @JsonProperty("return")
    private String returnUrl;
    private String url;
    private String paymentDetails;
    private String status;
    private String merchantId;
    private Double usdtRate;
    private Double cryptoAmount;

    public String getFinalUrl() {
        if (redirect != null && !redirect.isEmpty()) return redirect;
        return url;
    }
}
