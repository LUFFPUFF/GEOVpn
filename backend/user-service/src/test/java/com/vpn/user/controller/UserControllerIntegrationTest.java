package com.vpn.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vpn.user.domain.entity.User;
import com.vpn.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String INTERNAL_SECRET = "Internal_Secret_Vpn_App_2024_!@#";

    @BeforeEach
    void setup() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Безопасность: Должен возвращать ошибку 403 Forbidden, если отсутствует внутренний секретный ключ.")
    void shouldReturn403WhenSecretMissing() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("X-User-Id", "123"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Безопасность: Должен возвращать ошибку 403 Forbidden, если внутренний секретный ключ указан неверно.")
    void shouldReturn403WhenSecretWrong() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("X-Internal-Secret", "WRONG_SECRET")
                        .header("X-User-Id", "123"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Роль пользователя: Должен возвращать профиль авторизованного пользователя.")
    void shouldReturnProfileForUser() throws Exception {
        User user = User.builder()
                .telegramId(100L)
                .username("test_user")
                .balance(0)
                .build();
        userRepository.save(user);

        mockMvc.perform(get("/api/v1/users/me")
                        .header("X-Internal-Secret", INTERNAL_SECRET)
                        .header("X-User-Id", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.telegramId").value(100))
                .andExpect(jsonPath("$.data.username").value("test_user"));
    }

    @Test
    @DisplayName("Роль администратора: должна предоставлять доступ к административному endpoint'у")
    void shouldAllowAdminAccess() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .header("X-Internal-Secret", INTERNAL_SECRET)
                        .header("X-User-Id", "999999"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Роль администратора: Должен ЗАПРЕТИТЬ доступ к административному endpoint'у для обычного пользователя.")
    void shouldDenyAdminAccessForRegularUser() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .header("X-Internal-Secret", INTERNAL_SECRET)
                        .header("X-User-Id", "100"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Роль сервиса: Должна позволять списание средств с баланса без учета контекста пользователя.")
    void shouldAllowServiceAccess() throws Exception {
        User user = User.builder().telegramId(500L).balance(1000).build();
        userRepository.save(user);

        mockMvc.perform(post("/api/v1/users/500/deduct")
                        .header("X-Internal-Secret", INTERNAL_SECRET)
                        .param("amount", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balance").value(900));
    }
}