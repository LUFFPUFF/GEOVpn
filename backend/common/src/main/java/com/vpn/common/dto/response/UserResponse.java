package com.vpn.common.dto.response;

import com.vpn.common.dto.enums.SubscriptionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private Long telegramId;
    private String username;
    private String firstName;
    private Integer balance;
    private SubscriptionType subscriptionType;
    private LocalDateTime subscriptionExpiresAt;
    private String referralCode;
    private LocalDateTime createdAt;
    private LocalDateTime lastActiveAt;
    private boolean hasActiveSubscription;
}
