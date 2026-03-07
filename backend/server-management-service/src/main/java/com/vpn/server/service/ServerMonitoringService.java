package com.vpn.server.service;

import com.vpn.server.domain.entity.Server;
import com.vpn.server.grpc.XrayGrpcClient;
import com.vpn.server.repository.ServerRepository;
import com.vpn.server.util.NetworkUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServerMonitoringService {

    private final ServerRepository serverRepository;
    private final XrayGrpcClient xrayGrpcClient;
    private final NetworkUtils networkUtils;

    @Value("${vpn.monitoring.xray-grpc-port:10085}")
    private int xrayGrpcPort;

    @Value("${vpn.monitoring.timeout-ms:2000}")
    private int timeoutMs;

    private final ExecutorService healthCheckExecutor = Executors.newFixedThreadPool(10);

    @Scheduled(fixedDelayString = "${vpn.monitoring.health-check-interval-ms:30000}")
    @Transactional
    public void runHealthChecks() {
        log.info("Starting health checks for all active servers...");

        List<Server> activeServers = serverRepository.findByIsActiveTrue();

        List<CompletableFuture<Void>> futures = activeServers.stream()
                .map(server -> CompletableFuture.runAsync(() -> checkServer(server), healthCheckExecutor))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        log.info("Health checks completed. Checked {} servers.", activeServers.size());
    }

    /**
     * Проверка одного сервера
     */
    @Transactional
    public void checkServer(Server server) {
        long start = System.currentTimeMillis();
        boolean isReachable = false;
        int latency = 0;
        int estimatedLoad = server.getCurrentConnections();

        try {
            if (networkUtils.isPortOpen(server.getIpAddress(), server.getPort(), timeoutMs)) {
                latency = (int) (System.currentTimeMillis() - start);

                XrayGrpcClient.SysMetrics metrics = xrayGrpcClient.getSysMetrics(
                        server.getIpAddress(),
                        xrayGrpcPort
                );

                if (metrics != null) {
                    isReachable = true;
                    estimatedLoad = metrics.getNumGoroutine() / 4;

                    log.debug("Server {} metrics: latency={}ms, goroutines={}, uptime={}",
                            server.getName(), latency, metrics.getNumGoroutine(), metrics.getUptime());
                } else {
                    log.warn("Server {} TCP is open ({}ms), but Xray gRPC failed", server.getName(), latency);
                }
            } else {
                log.warn("Server {} is unreachable (TCP timeout)", server.getName());
            }

        } catch (Exception e) {
            log.error("Error checking server {}: {}", server.getName(), e.getMessage());
            isReachable = false;
        }

        server.updateHealthMetrics(isReachable, latency, estimatedLoad);
        serverRepository.save(server);
    }
}
