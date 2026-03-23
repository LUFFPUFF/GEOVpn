package com.vpn.config.service.interf;



import com.vpn.common.dto.request.ConfigCreateRequest;
import com.vpn.common.dto.request.ConfigRegenerateRequest;
import com.vpn.common.dto.response.VpnConfigResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Главный сервис для управления VPN конфигурациями и подписками.
 * Поддерживает мульти-серверную архитектуру и каскадные реле (RU-Relay).
 */
public interface VpnConfigService {

    /**
     * Создать новую VPN подписку для устройства.
     * Генерирует UUID, выбирает оптимальный сервер и синхронизирует данные со всеми XUI панелями.
     *
     * @param request параметры создания (userId, deviceId и т.д.)
     * @return VpnConfigResponse со списком всех доступных серверов и ссылкой на подписку
     */
    VpnConfigResponse createConfig(ConfigCreateRequest request);

    /**
     * Получить контент подписки в формате Base64.
     * Используется внешними приложениями (Happ, V2Box, v2rayNG) для загрузки списка серверов.
     *
     * @param vlessUuid UUID пользователя
     * @return Строка в формате Base64, содержащая список VLESS/Hysteria2 ссылок
     */
    String getSubscription(UUID vlessUuid);

    /**
     * Получить полную информацию о конфигурации по ID устройства.
     */
    VpnConfigResponse getConfigByDeviceId(Long deviceId);

    /**
     * Получить информацию о конфигурации по UUID.
     */
    VpnConfigResponse getConfigByVlessUuid(UUID vlessUuid);

    /**
     * Перегенерировать подписку (выпуск нового UUID и отзыв старого).
     */
    VpnConfigResponse regenerateConfig(Long deviceId, ConfigRegenerateRequest request);

    /**
     * Отозвать подписку (удалить пользователя со всех серверов и пометить как REVOKED).
     */
    void revokeConfig(Long deviceId, Long userId);

    /**
     * Получить все активные подписки пользователя (для разных устройств).
     */
    List<VpnConfigResponse> getActiveConfigs(Long userId);

    /**
     * Обновить время последнего использования.
     */
    void updateLastUsed(UUID vlessUuid);

    /**
     * Проверить, принадлежит ли UUID/Конфигурация данному пользователю.
     */
    boolean isConfigOwnedByUser(Long deviceId, Long userId);
}
