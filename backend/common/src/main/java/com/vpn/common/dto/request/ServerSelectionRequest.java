package com.vpn.common.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Запрос на выбор оптимального сервера
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerSelectionRequest {

    private Long userId;
    private String preferredCountry;
    private String protocol;

    private String userLocation;
}
