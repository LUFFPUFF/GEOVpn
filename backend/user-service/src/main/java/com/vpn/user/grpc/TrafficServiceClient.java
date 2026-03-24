package com.vpn.user.grpc;

import com.vpn.common.config.FeignClientConfiguration;
import com.vpn.common.dto.ApiResponse;
import com.vpn.common.dto.TrafficSessionDto;
import com.vpn.common.dto.TrafficSummaryDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * Feign-клиент для получения статистики трафика из server-service.
 */
@FeignClient(name = "server-management-service", url = "http://localhost:8084", configuration = FeignClientConfiguration.class)
public interface TrafficServiceClient {

    @GetMapping("/api/v1/traffic/users/{userId}/summary")
    ApiResponse<TrafficSummaryDto> getTrafficSummary(@PathVariable("userId") Long userId);

    @GetMapping("/api/v1/traffic/users/{userId}/sessions")
    ApiResponse<List<TrafficSessionDto>> getRecentSessions(@PathVariable("userId") Long userId);
}
