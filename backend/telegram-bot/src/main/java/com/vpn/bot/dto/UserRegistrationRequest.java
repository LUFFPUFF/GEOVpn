package com.vpn.bot.dto;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserRegistrationRequest {
    private Long telegramId;
    private String username;
    private String firstName;
    private String referralCode;
}