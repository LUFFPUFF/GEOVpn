package com.vpn.config.service.interf;

import com.vpn.common.dto.ServerDto;
import com.vpn.common.dto.request.ServerSelectionRequest;
import com.vpn.common.dto.ServerSelectionResult;

import java.util.List;

/**
 * Сервис для выбора оптимального VPN сервера
 */
public interface ServerSelectionService {

    /**
     * Выбрать лучший сервер на основе weighted scoring
     *
     * @param request параметры выбора
     * @return результат с выбранным сервером и метриками
     */
    ServerSelectionResult selectBestServer(ServerSelectionRequest request);

    /**
     * Получить топ N серверов с лучшими показателями
     *
     * @param request параметры выбора
     * @param topN количество серверов
     * @return список серверов отсортированных по score
     */
    List<ServerSelectionResult> getTopServers(ServerSelectionRequest request, int topN);

    /**
     * Рассчитать score для конкретного сервера
     *
     * @param server сервер для оценки
     * @param userLocation локация пользователя
     * @return результат с score breakdown
     */
    ServerSelectionResult calculateServerScore(ServerDto server, String userLocation);

    /**
     * Получить все активные серверы
     */
    List<ServerDto> getAllActiveServers();
}
