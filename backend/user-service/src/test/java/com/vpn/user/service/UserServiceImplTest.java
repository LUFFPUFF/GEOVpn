package com.vpn.user.service;

import com.vpn.user.domain.entity.User;
import com.vpn.user.dto.mapper.UserMapper;
import com.vpn.user.dto.request.UserRegistrationRequest;
import com.vpn.user.dto.response.UserResponse;
import com.vpn.user.exception.DuplicateUserException;
import com.vpn.user.exception.InsufficientBalanceException;
import com.vpn.user.exception.UserNotFoundException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("Регистрация нового пользователя должна пройти успешно.")
    void shouldRegisterUserSuccessfully() {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setTelegramId(12345L);
        request.setUsername("test_user");

        User userEntity = User.builder()
                .telegramId(12345L)
                .username("test_user")
                .balance(5000)
                .build();

        UserResponse expectedResponse = new UserResponse();
        expectedResponse.setTelegramId(12345L);
        expectedResponse.setBalance(5000);

        when(userRepository.existsByTelegramId(12345L)).thenReturn(false);
        when(userRepository.findByReferralCode(anyString())).thenReturn(Optional.empty());
        when(userMapper.toEntity(request)).thenReturn(userEntity);
        when(userRepository.save(any(User.class))).thenReturn(userEntity);
        when(userMapper.toResponse(userEntity)).thenReturn(expectedResponse);

        UserResponse actualResponse = userService.registerUser(request);

        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getTelegramId()).isEqualTo(12345L);
        assertThat(actualResponse.getBalance()).isEqualTo(5000);

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("При регистрации существующего пользователя должно генерироваться исключение DuplicateUserException.")
    void shouldThrowDuplicateUserException() {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setTelegramId(12345L);

        when(userRepository.existsByTelegramId(12345L)).thenReturn(true);

        assertThatThrownBy(() -> userService.registerUser(request))
                .isInstanceOf(DuplicateUserException.class)
                .hasMessageContaining("12345");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Списание остатка должно пройти успешно.")
    void shouldDeductBalanceSuccessfully() {
        Long telegramId = 12345L;
        Integer amount = 1000;

        User user = User.builder()
                .telegramId(telegramId)
                .balance(5000)
                .build();

        when(userRepository.findByTelegramId(telegramId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            return UserResponse.builder().balance(u.getBalance()).build();
        });

        UserResponse response = userService.deductBalance(telegramId, amount);

        assertThat(response.getBalance()).isEqualTo(4000);
        assertThat(user.getBalance()).isEqualTo(4000);
    }

    @Test
    @DisplayName("Следует выбрасывать исключение InfuficientBalanceException при низком балансе.")
    void shouldThrowInsufficientBalanceException() {
        Long telegramId = 12345L;
        Integer amount = 6000;

        User user = User.builder()
                .telegramId(telegramId)
                .balance(5000)
                .build();

        when(userRepository.findByTelegramId(telegramId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.deductBalance(telegramId, amount))
                .isInstanceOf(InsufficientBalanceException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Следует выбрасывать исключение UserNotFoundException, если пользователь не существует.")
    void shouldThrowUserNotFoundException() {
        Long telegramId = 999L;
        when(userRepository.findByTelegramId(telegramId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserByTelegramId(telegramId))
                .isInstanceOf(UserNotFoundException.class);
    }

}