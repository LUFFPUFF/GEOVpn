package com.vpn.billing.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DepositRequest {

    @NotNull(message = "Сумма пополнения обязательна")
    @Min(value = 10, message = "Минимальная сумма пополнения 75 рублей")
    private Integer amount;

    private Integer paymentMethod;
}
