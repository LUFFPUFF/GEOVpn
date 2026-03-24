package com.vpn.server.controller;

import com.vpn.common.dto.ApiResponse;
import com.vpn.common.dto.TrafficSessionDto;
import com.vpn.common.dto.TrafficSummaryDto;
import com.vpn.common.dto.projection.TrafficSummaryProjection;
import com.vpn.server.repository.TrafficUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public ApiResponse<TrafficSummaryDto> getTrafficSummary(@PathVariable Long userId) {
        TrafficSummaryProjection projection = trafficRepository.sumTrafficByUserId(userId);

        Long bytesIn = (projection != null) ? projection.getBytesIn() : 0L;
        Long bytesOut = (projection != null) ? projection.getBytesOut() : 0L;

        Long costKopecks = trafficRepository.sumCostByUserId(userId);

        TrafficSummaryDto summary = TrafficSummaryDto.builder()
                .bytesIn(bytesIn)
                .bytesOut(bytesOut)
                .costKopecks(costKopecks != null ? costKopecks : 0L)
                .build();

        return ApiResponse.success(summary);
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
}
