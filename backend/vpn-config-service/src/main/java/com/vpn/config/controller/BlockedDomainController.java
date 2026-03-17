package com.vpn.config.controller;

import com.vpn.common.dto.ApiResponse;
import com.vpn.common.security.annotations.RequireService;
import com.vpn.config.domain.entity.BlockedDomain;
import com.vpn.config.repository.BlockedDomainRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/domains")
@RequiredArgsConstructor
public class BlockedDomainController {

    private final BlockedDomainRepository domainRepository;

    /**
     * Отдает плоский список всех заблокированных доменов (без масок).
     * Используется Android-клиентом для Smart Routing (split tunneling).
     *
     * @return List<String> с именами доменов (напр. ["instagram.com", "facebook.com"])
     */
    @GetMapping("/blocked/flat")
    @RequireService
    @Cacheable(value = "blocked-domains-flat", key = "'all'")
    public ResponseEntity<ApiResponse<List<String>>> getFlatBlockedDomains() {
        log.info("Запрошен плоский список заблокированных доменов");

        long startTime = System.currentTimeMillis();

        List<String> domains = domainRepository.findByIsBlockedTrue()
                .stream()
                .map(BlockedDomain::getDomain)
                .collect(Collectors.toList());

        long duration = System.currentTimeMillis() - startTime;
        log.info("Список из {} доменов собран за {} мс", domains.size(), duration);

        return ResponseEntity.ok(ApiResponse.success(domains));
    }
}
