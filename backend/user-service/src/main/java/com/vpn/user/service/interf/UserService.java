package com.vpn.user.service.interf;

import com.vpn.common.dto.request.UserRegistrationRequest;
import com.vpn.common.dto.response.LeaderboardEntryDto;
import com.vpn.common.dto.response.TrafficStatsResponse;
import com.vpn.common.dto.response.UserResponse;
import com.vpn.common.dto.response.UserStatsResponse;
import com.vpn.common.dto.request.UserUpdateRequest;

import java.util.List;

public interface UserService {

    /**
     * Регистрация нового пользователя через Telegram
     */
    UserResponse registerUser(UserRegistrationRequest request);

    /**
     * Получить пользователя по Telegram ID
     */
    UserResponse getUserByTelegramId(Long telegramId);

    /**
     * Получить пользователя по внутреннему ID
     */
    UserResponse getUserById(Long id);

    /**
     * Обновить профиль пользователя
     */
    UserResponse updateUser(Long telegramId, UserUpdateRequest request);

    /**
     * Удалить пользователя (soft delete - деактивация)
     */
    void deactivateUser(Long telegramId);

    /**
     * Добавить баланс пользователю
     */
    UserResponse addBalance(Long telegramId, Integer amount);

    /**
     * Списать баланс пользователя
     */
    UserResponse deductBalance(Long telegramId, Integer amount);

    /**
     * Получить статистику пользователя
     */
    UserStatsResponse getUserStats(Long telegramId);

    /**
     * Проверить существование пользователя
     */
    boolean userExists(Long telegramId);

    /**
     * Получить всех пользователей с пагинацией
     */
    List<UserResponse> getAllUsers(int page, int size);

    /**
     * Обновить время последней активности
     */
    void updateLastActive(Long telegramId);

    TrafficStatsResponse getTrafficStats(Long telegramId);

    UserResponse purchaseSubscription(Long telegramId, String planName, int months);

    List<LeaderboardEntryDto> getLeaderboard();
}
