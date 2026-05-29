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

    /**
     * Физическое удаление устройства из БД.
     * Вызывается пользователем — проверяет право владения по UUID.
     */
    void deleteDevice(UUID uuid, Long telegramId);

    /**
     * Физическое удаление устройства по внутреннему ID.
     * Вызывается из config-service через DeviceServiceClient при принудительном
     * соблюдении лимита. Не выбрасывает исключение, если устройство не найдено.
     */
    void deleteDeviceById(Long deviceId, Long userId);

    int getMaxDevicesForUser(Long userId);
}
