package org.geovpn.bot.dto;
import lombok.Data;

@Data
public class UserResponse {
    private Long telegramId;
    private Integer balance;
    private String subscriptionType;
    private Boolean hasActiveSubscription;
    private String referralCode;
}