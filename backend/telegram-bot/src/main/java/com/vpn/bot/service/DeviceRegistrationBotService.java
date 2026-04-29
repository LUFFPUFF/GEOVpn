package com.vpn.bot.service;

import com.vpn.bot.client.UserServiceClient;
import com.vpn.common.dto.enums.DeviceType;
import com.vpn.common.dto.request.DeviceCreateRequest;
import com.vpn.common.dto.request.UserRegistrationRequest;
import com.vpn.common.dto.response.UserResponse;
import com.vpn.common.dto.ApiResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.User;

/**
 * Мост между Telegram-ботом и VpnConfigService.
 *
 * Инкапсулирует логику создания первого устройства при /start
 * и добавления дополнительных устройств.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceRegistrationBotService {

    private final UserServiceClient userServiceClient;

    @Value("${services.internal-secret}")
    private String internalSecret;

    public void registerUserIfAbsent(User tgUser) {
        try {
            log.info("🔍 Checking user existence: tgId={}", tgUser.getId());

            ApiResponse<UserResponse> response = userServiceClient.getUserByTelegramId(internalSecret, tgUser.getId());

            if (response == null || !response.isSuccess() || response.getData() == null || response.getData().getTelegramId() == null) {
                log.info("User {} NOT found in DB. Proceeding to registration...", tgUser.getId());
                registerNewUser(tgUser);
            } else {
                log.info("User {} exists in DB (ID: {})", tgUser.getId(), response.getData().getTelegramId());
            }
        } catch (Exception e) {
            log.warn("Error finding user {}, likely not registered. Error: {}", tgUser.getId(), e.getMessage());
            registerNewUser(tgUser);
        }
    }

    private void registerNewUser(User tgUser) {
        try {
            UserRegistrationRequest request = UserRegistrationRequest.builder()
                    .telegramId(tgUser.getId())
                    .username(tgUser.getUserName() != null ? tgUser.getUserName() : "user_" + tgUser.getId())
                    .firstName(tgUser.getFirstName())
                    .build();

            log.info("Requesting POST /api/v1/users/register for tgId={}", tgUser.getId());
            ApiResponse<UserResponse> regResponse = userServiceClient.registerUser(request);

            if (regResponse != null && regResponse.isSuccess()) {
                log.info("SUCCESS: User {} registered in DB.", tgUser.getId());
            } else {
                log.error("FAILED: User-service returned error during registration");
            }
        } catch (Exception e) {
            log.error("CRITICAL: Could not register user {}: {}", tgUser.getId(), e.getMessage());
        }
    }

    public void registerDeviceOnly(Long tgId, String os) {
        try {
            DeviceType type = mapOsToDeviceType(os);

            DeviceCreateRequest request = DeviceCreateRequest.builder()
                    .userId(tgId)
                    .deviceName(os + " · Device")
                    .deviceType(type)
                    .build();

            userServiceClient.registerDevice(internalSecret, tgId, request);
            log.info("Device {} registered for user {}", type, tgId);
        } catch (Exception e) {
            log.error("Failed to register device: {}", e.getMessage());
        }
    }

    private DeviceType mapOsToDeviceType(String os) {
        return switch (os.toUpperCase()) {
            case "IOS" -> DeviceType.IOS;
            case "ANDROID" -> DeviceType.ANDROID;
            case "WINDOWS" -> DeviceType.WINDOWS;
            case "MACOS" -> DeviceType.MACOS;
            default -> DeviceType.OTHER;
        };
    }
}