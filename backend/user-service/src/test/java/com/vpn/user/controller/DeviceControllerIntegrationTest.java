package com.vpn.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vpn.user.domain.entity.Device;
import com.vpn.user.domain.entity.DeviceType;
import com.vpn.user.domain.entity.User;
import com.vpn.user.dto.request.DeviceCreateRequest;
import com.vpn.user.repository.DeviceRepository;
import com.vpn.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeviceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String INTERNAL_SECRET = "Internal_Secret_Vpn_App_2024_!@#";

    private User testUser;

    @BeforeEach
    void setup() {
        deviceRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .telegramId(12345L)
                .username("device_tester")
                .balance(1000)
                .build();
        userRepository.save(testUser);
    }

    @Test
    @DisplayName("Создание устройства: Новое устройство должно быть создано успешно.")
    void shouldCreateDeviceSuccessfully() throws Exception {
        DeviceCreateRequest request = new DeviceCreateRequest();
        request.setDeviceName("iPhone 15");
        request.setDeviceType(DeviceType.IOS);

        mockMvc.perform(post("/api/v1/devices")
                        .header("X-Internal-Secret", INTERNAL_SECRET)
                        .header("X-User-Id", String.valueOf(testUser.getTelegramId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.deviceName").value("iPhone 15"))
                .andExpect(jsonPath("$.data.uuid").isNotEmpty());

        assertThat(deviceRepository.countActiveDevicesByUserId(testUser.getTelegramId())).isEqualTo(1);
    }

    @Test
    @DisplayName("Ограничение на количество устройств: при превышении лимита должен возвращаться код 400.")
    void shouldReturn400WhenLimitExceeded() throws Exception {
        for (int i = 0; i < 5; i++) {
            deviceRepository.save(Device.builder()
                    .userId(testUser.getTelegramId())
                    .deviceName("Device " + i)
                    .deviceType(DeviceType.ANDROID)
                    .uuid(UUID.randomUUID())
                    .isActive(true)
                    .build());
        }

        DeviceCreateRequest request = new DeviceCreateRequest();
        request.setDeviceName("Overflow Device");
        request.setDeviceType(DeviceType.DESKTOP);

        mockMvc.perform(post("/api/v1/devices")
                        .header("X-Internal-Secret", INTERNAL_SECRET)
                        .header("X-User-Id", String.valueOf(testUser.getTelegramId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()) // 400
                .andExpect(jsonPath("$.error.code").value("USR_003"));
    }

    @Test
    @DisplayName("Безопасность (IDOR): При доступе к устройству другого пользователя должен возвращаться код 404.")
    void shouldReturn404ForForeignDevice() throws Exception {
        UUID foreignUuid = UUID.randomUUID();
        deviceRepository.save(Device.builder()
                .userId(999L)
                .deviceName("Hacker Phone")
                .deviceType(DeviceType.ANDROID)
                .uuid(foreignUuid)
                .isActive(true)
                .build());

        mockMvc.perform(get("/api/v1/devices/" + foreignUuid)
                        .header("X-Internal-Secret", INTERNAL_SECRET)
                        .header("X-User-Id", String.valueOf(testUser.getTelegramId())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("DEV_001"));
    }

    @Test
    @DisplayName("Деактивировать: При необходимости выполнить программное удаление устройства.")
    void shouldDeactivateDevice() throws Exception {
        UUID deviceUuid = UUID.randomUUID();
        deviceRepository.save(Device.builder()
                .userId(testUser.getTelegramId())
                .deviceName("My Laptop")
                .deviceType(DeviceType.DESKTOP)
                .uuid(deviceUuid)
                .isActive(true)
                .build());

        mockMvc.perform(delete("/api/v1/devices/" + deviceUuid)
                        .header("X-Internal-Secret", INTERNAL_SECRET)
                        .header("X-User-Id", String.valueOf(testUser.getTelegramId())))
                .andExpect(status().isOk());

        Device deviceInDb = deviceRepository.findByUuid(deviceUuid).orElseThrow();
        assertThat(deviceInDb.getIsActive()).isFalse();
    }

}