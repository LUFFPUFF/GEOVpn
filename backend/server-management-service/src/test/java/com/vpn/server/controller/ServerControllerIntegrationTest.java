package com.vpn.server.controller;

import com.vpn.server.ServerManagementApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ServerManagementApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ServerControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String INTERNAL_SECRET = "Internal_Secret_Vpn_App_2024_!@#";

    @Test
    @DisplayName("Следует ЗАПРЕТИТЬ доступ без секретного ключа")
    void shouldDenyWithoutSecret() throws Exception {
        mockMvc.perform(get("/api/v1/servers/active"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Необходимо разрешить доступ с использованием правильного секретного ключа  (Role SERVICE)")
    void shouldAllowWithSecret() throws Exception {
        mockMvc.perform(get("/api/v1/servers/active")
                        .header("X-Internal-Secret", INTERNAL_SECRET))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Должен быть РАЗРЕШЕН доступ для пользователя-АДМИНИСТРАТОРА")
    void shouldAllowAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/servers/active")
                        .header("X-Internal-Secret", INTERNAL_SECRET)
                        .header("X-User-Id", "999999"))
                .andExpect(status().isOk());
    }
}