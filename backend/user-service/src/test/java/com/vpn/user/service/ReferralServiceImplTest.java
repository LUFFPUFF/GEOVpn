package com.vpn.user.service;

import com.vpn.common.exception.ValidationException;
import com.vpn.user.domain.entity.User;
import com.vpn.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReferralServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReferralServiceImpl referralService;

    @Test
    @DisplayName("Успех: Обработка реферальных программ и начисление бонусов.")
    void shouldProcessReferralSuccessfully() {
        String refCode = "REF-123";
        User referrer = User.builder().telegramId(100L).balance(0).build();
        User newUser = User.builder().telegramId(200L).balance(0).build();

        when(userRepository.findByReferralCode(refCode)).thenReturn(Optional.of(referrer));
        when(userRepository.findByTelegramId(100L)).thenReturn(Optional.of(referrer));

        referralService.processReferral(newUser, refCode);

        assertThat(newUser.getReferredBy()).isEqualTo(100L);
        assertThat(newUser.getBalance()).isEqualTo(2500);
        assertThat(referrer.getBalance()).isEqualTo(5000);

        verify(userRepository).save(referrer);
    }

    @Test
    @DisplayName("Ошибка: Невозможно использовать собственный реферальный код.")
    void shouldThrowWhenUsingOwnCode() {
        String refCode = "REF-SELF";
        User user = User.builder().telegramId(100L).referralCode(refCode).build();

        when(userRepository.findByReferralCode(refCode)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> referralService.processReferral(user, refCode))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("own referral code");
    }

    @Test
    @DisplayName("Ошибка: Неверный реферальный код")
    void shouldThrowWhenCodeNotFound() {
        String refCode = "INVALID";
        User user = User.builder().telegramId(100L).build();

        when(userRepository.findByReferralCode(refCode)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> referralService.processReferral(user, refCode))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid referral code");
    }

}