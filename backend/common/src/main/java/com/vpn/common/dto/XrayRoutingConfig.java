package com.vpn.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Xray Routing Configuration
 *
 * Структура секции "routing" в Xray конфиге
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class XrayRoutingConfig {

    /**
     * Стратегия разрешения доменов
     * "AsIs" - как есть
     * "IPIfNonMatch" - резолвить в IP если нет совпадений по домену
     * "IPOnDemand" - резолвить только при необходимости
     */
    @JsonProperty("domainStrategy")
    @Builder.Default
    private String domainStrategy = "IPIfNonMatch";

    /**
     * Список routing правил
     */
    @JsonProperty("rules")
    private List<RoutingRule> rules;

    /**
     * Балансировщик нагрузки
     */
    @JsonProperty("balancers")
    private List<Object> balancers;
}
