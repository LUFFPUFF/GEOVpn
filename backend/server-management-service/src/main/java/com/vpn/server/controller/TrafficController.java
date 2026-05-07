package com.vpn.server.controller;

import com.vpn.common.dto.ApiResponse;
import com.vpn.common.dto.TrafficSessionDto;
import com.vpn.common.dto.TrafficSummaryDto;
import com.vpn.common.dto.projection.TrafficSummaryProjection;
import com.vpn.common.security.UserRole;
import com.vpn.common.security.annotations.RequireAnyRole;
import com.vpn.common.security.context.SecurityContextHolder;
import com.vpn.server.dto.UserTrafficStatsDto;
import com.vpn.server.repository.TrafficUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/traffic")
@RequiredArgsConstructor
public class TrafficController {

    private final TrafficUsageRepository trafficRepository;

    @GetMapping("/users/{userId}/summary")
    @RequireAnyRole({UserRole.USER, UserRole.ADMIN, UserRole.SERVICE})
    public ResponseEntity<ApiResponse<TrafficSummaryDto>> getTrafficSummary(@PathVariable Long userId) {

        validateAccessToUser(userId);

        TrafficSummaryProjection projection = trafficRepository.sumTrafficByUserId(userId);
        Long bytesIn = (projection != null) ? projection.getBytesIn() : 0L;
        Long bytesOut = (projection != null) ? projection.getBytesOut() : 0L;
        Long costKopecks = trafficRepository.sumCostByUserId(userId);

        TrafficSummaryDto summary = TrafficSummaryDto.builder()
                .bytesIn(bytesIn)
                .bytesOut(bytesOut)
                .costKopecks(costKopecks != null ? costKopecks : 0L)
                .build();

        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/users/{userId}/sessions")
    public ApiResponse<List<TrafficSessionDto>> getRecentSessions(@PathVariable Long userId) {
        List<TrafficSessionDto> sessions = trafficRepository.findByUserIdOrderByCollectedAtDesc(userId)
                .stream()
                .limit(50)
                .map(t -> TrafficSessionDto.builder()
                        .serverId(t.getServerId())
                        .bytesIn(t.getBytesIn())
                        .bytesOut(t.getBytesOut())
                        .costKopecks(Long.valueOf(t.getCostKopecks()))
                        .collectedAt(t.getCollectedAt())
                        .build())
                .collect(Collectors.toList());

        return ApiResponse.success(sessions);
    }

    @GetMapping("/servers/{serverId}/stats")
    @RequireAnyRole({UserRole.ADMIN, UserRole.SERVICE})
    public ResponseEntity<ApiResponse<List<UserTrafficStatsDto>>> getServerTrafficStats(@PathVariable Integer serverId) {
        var stats = trafficRepository.findTrafficByServer(serverId);
        List<UserTrafficStatsDto> result = stats.stream().map(s ->
                UserTrafficStatsDto.builder()
                        .userId(s.getUserId())
                        .totalUp(s.getBytesIn())
                        .totalDown(s.getBytesOut())
                        .build()
        ).collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    private void validateAccessToUser(Long targetUserId) {
        Long currentUserId = SecurityContextHolder.getUserId();

        if (SecurityContextHolder.isAdmin() || SecurityContextHolder.isService()) {
            return;
        }

        if (!targetUserId.equals(currentUserId)) {
            log.warn("Попытка несанкционированного доступа: Юзер {} пытался посмотреть трафик юзера {}", currentUserId, targetUserId);
            throw new com.vpn.common.exception.ForbiddenException("Вы можете просматривать только свою статистику");
        }
    }
}
