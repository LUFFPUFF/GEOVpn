package com.vpn.user.service.interf;

import com.vpn.user.domain.entity.User;

/**
 * Сервис для управления реферальной системой
 */
public interface ReferralService {

    /**
     * Обработать реферальный код при регистрации
     *
     * @param newUser новый пользователь
     * @param referralCode реферальный код пригласившего
     */
    void processReferral(User newUser, String referralCode);

    /**
     * Начислить реферальный бонус
     *
     * @param referrerTelegramId Telegram ID пригласившего
     * @param referredTelegramId Telegram ID приглашенного
     * @param bonusAmount сумма бонуса в копейках
     */
    void awardReferralBonus(Long referrerTelegramId, Long referredTelegramId, Integer bonusAmount);
}
