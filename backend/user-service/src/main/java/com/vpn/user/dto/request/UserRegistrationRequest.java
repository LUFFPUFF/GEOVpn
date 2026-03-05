package com.vpn.user.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationRequest {

    @NotNull(message = "Telegram ID is required")
    @Positive(message = "Telegram ID must be positive")
    private Long telegramId;

    private String username;

    private String firstName;

    private String referralCode;
}
