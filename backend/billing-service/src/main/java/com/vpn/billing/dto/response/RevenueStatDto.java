package com.vpn.billing.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RevenueStatDto {
    private String date;
    private Long amountRub;
}
