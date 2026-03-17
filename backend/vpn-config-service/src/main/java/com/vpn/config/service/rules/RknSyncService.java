package com.vpn.config.service.rules;

import com.vpn.config.domain.entity.BlockedDomain;
import com.vpn.config.domain.enums.BlockSource;
import com.vpn.config.domain.enums.DomainCategory;
import com.vpn.config.domain.enums.MatchType;
import com.vpn.config.repository.BlockedDomainRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class RknSyncService {

    private final BlockedDomainRepository domainRepository;
    private final RknDataProvider dataProvider;
    private final CacheManager cacheManager;

    @Value("${service.vpn.rkn.batch-size:2000}")
    private int batchSize;

    @Transactional
    public int syncBlockedDomains() {
        log.info("Начало синхронизации списков блокировок...");
        long startTime = System.currentTimeMillis();

        try {
            Set<String> rawDomains = dataProvider.fetchBlockedDomains();
            log.info("Загружено сырых доменов: {}", rawDomains.size());

            if (rawDomains.isEmpty()) return 0;

            Map<String, MatchType> cleanDomainsMap = new HashMap<>();
            for (String rawDomain : rawDomains) {
                String cleanDomain = rawDomain;
                MatchType type = MatchType.EXACT;

                if (cleanDomain.startsWith("*.")) {
                    cleanDomain = cleanDomain.substring(2);
                    type = MatchType.WILDCARD;
                }
                cleanDomainsMap.put(cleanDomain, type);
            }

            Set<String> existingDomains = domainRepository.findAll()
                    .stream()
                    .map(BlockedDomain::getDomain)
                    .collect(Collectors.toSet());

            cleanDomainsMap.keySet().removeAll(existingDomains);
            int newDomainsCount = cleanDomainsMap.size();

            log.info("Новых уникальных доменов для добавления: {}", newDomainsCount);

            if (newDomainsCount == 0) return 0;

            List<BlockedDomain> batch = new ArrayList<>(batchSize);
            int processed = 0;

            for (Map.Entry<String, MatchType> entry : cleanDomainsMap.entrySet()) {
                batch.add(createRknDomainEntity(entry.getKey(), entry.getValue()));

                if (batch.size() >= batchSize) {
                    domainRepository.saveAll(batch);
                    domainRepository.flush();
                    processed += batch.size();
                    log.debug("💾 Сохранено {} / {}", processed, newDomainsCount);
                    batch.clear();
                }
            }

            if (!batch.isEmpty()) {
                domainRepository.saveAll(batch);
                processed += batch.size();
            }

            clearCaches();

            long duration = (System.currentTimeMillis() - startTime) / 1000;
            log.info("Синхронизация успешно завершена за {} сек. Добавлено: {}", duration, processed);

            return processed;

        } catch (Exception e) {
            log.error("Ошибка при синхронизации БД доменов: {}", e.getMessage(), e);
            throw new RuntimeException("Domain Sync Failed", e);
        }
    }

    private BlockedDomain createRknDomainEntity(String cleanDomain, MatchType matchType) {
        return BlockedDomain.builder()
                .domain(cleanDomain)
                .matchType(matchType)
                .isBlocked(true)
                .autoDetected(true)
                .source(BlockSource.ROSKOMNADZOR)
                .category(guessCategory(cleanDomain))
                .lastChecked(LocalDateTime.now())
                .build();
    }

    private DomainCategory guessCategory(String domain) {
        if (domain.matches(".*(instagram|facebook|twitter|x\\.com|twimg).*")) return DomainCategory.SOCIAL;
        if (domain.matches(".*(porn|sex|xxx|xvideos|pornhub).*")) return DomainCategory.ADULT;
        if (domain.matches(".*(bbc|news|meduza|svoboda).*")) return DomainCategory.MEDIA;
        if (domain.matches(".*(discord|signal).*")) return DomainCategory.MESSENGER;
        return DomainCategory.OTHER;
    }

    /**
     * Очищает все кеши после синхронизации доменов
     */
    private void clearCaches() {
        log.info("Очистка кешей после синхронизации...");

        try {
            if (cacheManager.getCache("routing-rules") != null) {
                Objects.requireNonNull(cacheManager.getCache("routing-rules")).clear();
                log.debug("Кеш routing-rules очищен");
            }

            if (cacheManager.getCache("blocked-domains-flat") != null) {
                Objects.requireNonNull(cacheManager.getCache("blocked-domains-flat")).clear();
                log.debug("Кеш blocked-domains-flat очищен");
            }

            log.info("Все кеши успешно очищены");

        } catch (Exception e) {
            log.warn("Ошибка при очистке кешей: {}", e.getMessage());
        }
    }
}
