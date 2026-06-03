package com.vpn.config.client;

import com.vpn.common.dto.ApiResponse;
import com.vpn.common.dto.ServerDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(
        name = "server-management-service",
        url = "${SERVER_MGM_SERVICE_URL}",
        configuration = com.vpn.common.config.FeignClientConfiguration.class
)
public interface ServerManagementClient {

    @GetMapping("/api/v1/servers/active")
    ApiResponse<List<ServerDto>> getActiveServers();

    @GetMapping("/api/v1/servers/{id}")
    ApiResponse<ServerDto> getServerById(@PathVariable("id") Integer id);

    @GetMapping("/api/v1/servers/{id}/health")
    ServerDto getServerHealth(@PathVariable("id") Integer id);

    @PutMapping("/api/v1/servers/{id}")
    ApiResponse<ServerDto> updateServer(@PathVariable("id") Integer id, @RequestBody Map<String, Object> updateRequest);
}
