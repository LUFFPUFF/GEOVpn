package com.vpn.config.service.rules;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
public class RknDataProvider {

    private final RestTemplate restTemplate;

    @Value("${service.vpn.rkn.api-url:https://reestr.rublacklist.net/api/v3/domains/}")
    private String primaryApiUrl;

    @Value("${service.vpn.rkn.fallback-url:https://raw.githubusercontent.com/zapret-info/z-i/master/domains.txt}")
    private String fallbackTextUrl;

    public RknDataProvider(@Qualifier("rknRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Возвращает уникальный список заблокированных доменов
     */
    public Set<String> fetchBlockedDomains() {
        try {
            log.info("Запрашиваем домены из основного API: {}", primaryApiUrl);
            String[] domainsArray = restTemplate.getForObject(primaryApiUrl, String[].class);

            if (domainsArray != null && domainsArray.length > 0) {
                return cleanDomains(Arrays.asList(domainsArray));
            }
        } catch (Exception e) {
            log.warn("Основное API недоступно ({}). Переходим на резервный источник.", e.getMessage());
        }

        return fetchFromFallback();
    }

    private Set<String> fetchFromFallback() {
        try {
            log.info("Запрашиваем домены из резервного дампа: {}", fallbackTextUrl);
            String textDump = restTemplate.getForObject(fallbackTextUrl, String.class);

            if (textDump != null && !textDump.isEmpty()) {
                return cleanDomains(Arrays.asList(textDump.split("\n")));
            }
        } catch (Exception e) {
            log.error("Резервный источник тоже недоступен: {}", e.getMessage());
            throw new RuntimeException("All RKN data sources are down", e);
        }
        return new HashSet<>();
    }

    private Set<String> cleanDomains(Iterable<String> rawDomains) {
        Set<String> result = new HashSet<>();
        for (String d : rawDomains) {
            if (d != null && !d.trim().isEmpty() && !d.startsWith("#")) {
                result.add(d.trim().toLowerCase());
            }
        }
        return result;
    }
}
