package com.vpn.common.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Запрос на перегенерацию конфигурации
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigRegenerateRequest {

    private String reason;

    private String preferredCountry;

    @Builder.Default
    private Boolean forceServerChange = false;
}
