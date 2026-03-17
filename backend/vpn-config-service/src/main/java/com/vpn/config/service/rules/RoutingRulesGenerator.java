package com.vpn.config.service.rules;

import com.vpn.common.dto.RoutingRule;
import com.vpn.config.domain.entity.BlockedDomain;
import com.vpn.config.repository.BlockedDomainRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Генератор routing rules для Xray конфигурации
 *
 * Логика маршрутизации:
 * 1. Заблокированные домены (из БД) → proxy (через VPN)
 * 2. Российские домены/IP → direct (напрямую)
 * 3. BitTorrent → block
 * 4. Private IP → block
 * 5. Остальное → direct
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoutingRulesGenerator {

    private final BlockedDomainRepository blockedDomainRepository;

    /**
     * Генерирует полный список routing rules
     * Кешируется на 1 час (обновляется при синхронизации БД)
     */
    @Cacheable(value = "routing-rules", key = "'full'")
    public List<RoutingRule> generateRoutingRules() {
        log.info("Генерация routing rules...");

        List<RoutingRule> rules = new ArrayList<>();

        rules.add(createBlockedDomainsRule());

        //todo доработать и добавить систему автоматизации
        rules.add(RoutingRule.builder()
                .type("field")
                .ip(List.of("geoip:private"))
                .outboundTag("block")
                .priority(2)
                .build());

        rules.add(RoutingRule.builder()
                .type("field")
                .protocol(List.of("bittorrent"))
                .outboundTag("block")
                .priority(3)
                .build());

        rules.add(RoutingRule.builder()
                .type("field")
                .ip(List.of("geoip:ru"))
                .outboundTag("direct")
                .priority(4)
                .build());

        rules.add(RoutingRule.builder()
                .type("field")
                .domain(List.of(
                        "geosite:category-ru",
                        "domain:yandex.ru",
                        "domain:vk.com",
                        "domain:mail.ru",
                        "domain:ok.ru",
                        "domain:gosuslugi.ru",
                        "domain:sberbank.ru"
                ))
                .outboundTag("direct")
                .priority(5)
                .build());

        log.info("Сгенерировано {} routing rules", rules.size());

        return rules;
    }

    /**
     * Создать правило для заблокированных доменов
     * Получает список из БД и формирует domain list
     */
    private RoutingRule createBlockedDomainsRule() {

        List<BlockedDomain> blockedDomains = blockedDomainRepository.findByIsBlockedTrue();

        log.info("Загружено {} заблокированных доменов из БД", blockedDomains.size());

        List<String> domainList = new ArrayList<>(blockedDomains.stream()
                .limit(10000)
                .map(bd -> {
                    String domain = bd.getDomain();

                    return switch (bd.getMatchType()) {
                        case WILDCARD -> "domain:" + domain;
                        case EXACT -> "full:" + domain;
                        default -> "domain:" + domain;
                    };
                })
                .toList());

        //todo доработать и добавить систему автоматизации
        domainList.addAll(List.of(
                "domain:instagram.com",
                "domain:facebook.com",
                "domain:twitter.com",
                "domain:x.com",
                "domain:youtube.com",
                "domain:discord.com",
                "domain:medium.com"
        ));

        return RoutingRule.builder()
                .type("field")
                .domain(domainList)
                .outboundTag("proxy")
                .priority(1)
                .build();
    }

    /**
     * Генерирует упрощенные правила (только топ домены)
     * Для использования в VLESS ссылке с base64 конфигом
     */
    public List<RoutingRule> generateSimplifiedRules() {
        log.info("Генерация упрощенных routing rules (топ домены)...");

        //todo доработать и добавить больше
        List<RoutingRule> rules = new ArrayList<>();

        List<String> topBlockedDomains = List.of(
                "domain:instagram.com",
                "domain:facebook.com",
                "domain:twitter.com",
                "domain:x.com",
                "domain:youtube.com",
                "domain:discord.com",
                "domain:medium.com",
                "domain:linkedin.com",
                "domain:spotify.com",
                "domain:telegram.org"
        );

        rules.add(RoutingRule.builder()
                .type("field")
                .domain(topBlockedDomains)
                .outboundTag("proxy")
                .priority(1)
                .build());

        rules.add(RoutingRule.builder()
                .type("field")
                .domain(List.of("geosite:category-ru"))
                .outboundTag("direct")
                .priority(2)
                .build());

        rules.add(RoutingRule.builder()
                .type("field")
                .ip(List.of("geoip:private"))
                .outboundTag("block")
                .priority(3)
                .build());

        return rules;
    }

    public void clearCache() {
        log.info("Очистка кеша routing rules");
        // Добавить очитску из RedisService
    }
}
