package com.vpn.config.service.interf;



import com.vpn.common.dto.request.ConfigCreateRequest;
import com.vpn.common.dto.request.ConfigRegenerateRequest;
import com.vpn.common.dto.response.VpnConfigResponse;

import java.util.List;
import java.util.UUID;

/**
 * Главный сервис для управления VPN конфигурациями
 */
public interface VpnConfigService {

    /**
     * Создать новую VPN конфигурацию для устройства
     *
     * @param request параметры создания
     * @return созданная конфигурация с VLESS ссылкой и QR кодом
     */
    VpnConfigResponse createConfig(ConfigCreateRequest request);

    /**
     * Получить конфигурацию по device ID
     *
     * @param deviceId ID устройства
     * @return конфигурация
     */
    VpnConfigResponse getConfigByDeviceId(Long deviceId);

    /**
     * Получить конфигурацию по VLESS UUID
     *
     * @param vlessUuid UUID из VLESS ссылки
     * @return конфигурация
     */
    VpnConfigResponse getConfigByVlessUuid(UUID vlessUuid);

    /**
     * Перегенерировать конфигурацию (новый UUID, новый сервер)
     *
     * @param deviceId ID устройства
     * @param request параметры регенерации
     * @return новая конфигурация
     */
    VpnConfigResponse regenerateConfig(Long deviceId, ConfigRegenerateRequest request);

    /**
     * Отозвать конфигурацию (revoke)
     *
     * @param deviceId ID устройства
     * @param userId ID пользователя (для проверки владельца)
     */
    void revokeConfig(Long deviceId, Long userId);

    /**
     * Получить все активные конфигурации пользователя
     *
     * @param userId ID пользователя
     * @return список конфигураций
     */
    List<VpnConfigResponse> getActiveConfigs(Long userId);

    /**
     * Обновить время последнего использования конфигурации
     *
     * @param vlessUuid UUID конфигурации
     */
    void updateLastUsed(UUID vlessUuid);

    /**
     * Проверить принадлежность конфигурации пользователю
     *
     * @param deviceId ID устройства
     * @param userId ID пользователя
     * @return true если принадлежит
     */
    boolean isConfigOwnedByUser(Long deviceId, Long userId);
}
