package com.vpn.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO для routing правила в Xray конфигурации
 *
 * Используется для генерации JSON конфига с маршрутизацией
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutingRule {

    @Builder.Default
    private String type = "field";

    /**
     * Список доменов для этого правила
     * Пример: ["instagram.com", "facebook.com"]
     */
    private List<String> domain;

    /**
     * Список IP адресов/подсетей
     * Пример: ["geoip:ru", "geoip:private"]
     */
    private List<String> ip;

    /**
     * Список протоколов
     * Пример: ["bittorrent"]
     */
    private List<String> protocol;

    /**
     * Куда направлять трафик
     * "proxy" - через VPN
     * "direct" - напрямую
     * "block" - блокировать
     */
    private String outboundTag;

    /**
     * Приоритет правила
     */
    private Integer priority;
}
