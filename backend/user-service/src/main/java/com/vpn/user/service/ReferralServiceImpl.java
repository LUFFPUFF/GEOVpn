package com.vpn.user.service;

import com.vpn.common.exception.ValidationException;
import com.vpn.user.domain.entity.User;
import com.vpn.user.repository.UserRepository;
import com.vpn.user.service.interf.ReferralService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация реферальной системы
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReferralServiceImpl implements ReferralService {

    private final UserRepository userRepository;

    private static final int REFERRAL_BONUS = 5000; // 50 рублей
    private static final int REFERRED_USER_BONUS = 2500; // 25 рублей

    @Override
    @Transactional
    public void processReferral(User newUser, String referralCode) {
        log.info("Processing referral: newUser={}, referralCode={}",
                newUser.getTelegramId(), referralCode);

        User referrer = userRepository.findByReferralCode(referralCode)
                .orElseThrow(() -> new ValidationException("Invalid referral code: " + referralCode));

        if (referrer.getTelegramId().equals(newUser.getTelegramId())) {
            log.warn("User attempted to use own referral code: {}", newUser.getTelegramId());
            throw new ValidationException("Cannot use your own referral code");
        }

        newUser.setReferredBy(referrer.getTelegramId());

        awardReferralBonus(referrer.getTelegramId(), newUser.getTelegramId(), REFERRAL_BONUS);

        newUser.addBalance(REFERRED_USER_BONUS); //todo обработать, нужно ли?

        log.info("Referral processed: referrer={}, referred={}, referrerBonus={}, referredBonus={}",
                referrer.getTelegramId(), newUser.getTelegramId(), REFERRAL_BONUS, REFERRED_USER_BONUS);
    }

    @Override
    @Transactional
    public void awardReferralBonus(Long referrerTelegramId, Long referredTelegramId, Integer bonusAmount) {
        log.info("Awarding referral bonus: referrer={}, referred={}, amount={}",
                referrerTelegramId, referredTelegramId, bonusAmount);

        User referrer = userRepository.findByTelegramId(referrerTelegramId)
                .orElseThrow(() -> new ValidationException("Referrer not found"));

        referrer.addBalance(bonusAmount);
        userRepository.save(referrer);

        // TODO: Создать запись в transactions таблице для истории

        log.info("Referral bonus awarded: referrer={}, amount={}", referrerTelegramId, bonusAmount);
    }
}
