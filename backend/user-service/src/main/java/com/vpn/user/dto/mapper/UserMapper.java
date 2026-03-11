package com.vpn.user.dto.mapper;

import com.vpn.user.domain.entity.User;
import com.vpn.common.dto.request.UserRegistrationRequest;
import com.vpn.common.dto.response.UserResponse;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toEntity(UserRegistrationRequest request) {
        return User.builder()
                .telegramId(request.getTelegramId())
                .username(request.getUsername())
                .firstName(request.getFirstName())
                .build();
    }

    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .telegramId(user.getTelegramId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .balance(user.getBalance())
                .subscriptionType(user.getSubscriptionType())
                .subscriptionExpiresAt(user.getSubscriptionExpiresAt())
                .referralCode(user.getReferralCode())
                .createdAt(user.getCreatedAt())
                .lastActiveAt(user.getLastActiveAt())
                .hasActiveSubscription(user.hasActiveSubscription())
                .build();
    }
}
