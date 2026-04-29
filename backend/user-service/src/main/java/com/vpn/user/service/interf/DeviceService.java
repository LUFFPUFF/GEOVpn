package com.vpn.user.service.interf;

import com.vpn.common.dto.request.DeviceCreateRequest;
import com.vpn.common.dto.response.DeviceResponse;

import java.util.List;
import java.util.UUID;

/**
 * Сервис для управления устройствами пользователей
 */
public interface DeviceService {

    /**
     * Создать новое устройство
     */
    DeviceResponse createDevice(DeviceCreateRequest request);

    /**
     * Получить устройство по UUID
     */
    DeviceResponse getDeviceByUuid(UUID uuid);

    /**
     * Получить все активные устройства пользователя
     */
    List<DeviceResponse> getUserActiveDevices(Long telegramId);

    /**
     * Деактивировать устройство
     */
    void deactivateDevice(UUID uuid, Long telegramId);

    /**
     * Обновить время последнего подключения
     */
    void updateLastConnected(UUID uuid);

    /**
     * Проверить принадлежность устройства пользователю
     */
    boolean isDeviceOwnedByUser(UUID deviceUuid, Long telegramId);

    DeviceResponse syncDeviceWithPlatform(Long userId, String platform);
}
