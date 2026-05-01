package com.vpn.user.service.interf;

import com.vpn.common.dto.response.LeaderboardEntryDto;
import com.vpn.common.dto.response.TrafficStatsResponse;
import com.vpn.common.dto.response.UserResponse;
import com.vpn.common.dto.response.UserStatsResponse;
import com.vpn.common.dto.request.UserRegistrationRequest;
import com.vpn.common.dto.request.UserUpdateRequest;

import java.util.List;

public interface UserService {

    UserResponse registerUser(UserRegistrationRequest request);

    UserResponse getUserByTelegramId(Long telegramId);

    UserResponse getUserById(Long id);

    UserResponse updateUser(Long telegramId, UserUpdateRequest request);

    void deactivateUser(Long telegramId);

    UserResponse addBalance(Long telegramId, Integer amount);

    UserResponse deductBalance(Long telegramId, Integer amount);

    UserStatsResponse getUserStats(Long telegramId);

    TrafficStatsResponse getTrafficStats(Long telegramId);

    /**
     * Обычная покупка подписки (без промо).
     */
    UserResponse purchaseSubscription(Long telegramId, String planName, int months);

    /**
     * Покупка подписки с поддержкой промо-акции.
     *
     * @param promo true — выдать бесплатный месяц BASIC (только для PAYG-пользователей)
     */
    UserResponse purchaseSubscription(Long telegramId, String planName, int months, boolean promo);

    boolean userExists(Long telegramId);

    List<UserResponse> getAllUsers(int page, int size);

    void updateLastActive(Long telegramId);

    List<LeaderboardEntryDto> getLeaderboard();
}