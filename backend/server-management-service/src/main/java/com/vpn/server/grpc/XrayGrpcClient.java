package com.vpn.server.grpc;

import com.vpn.server.grpc.stats.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class XrayGrpcClient {

    @Data
    @Builder
    public static class SysMetrics {
        private long uptime;
        private int numGoroutine;
    }

    /**
     * Получить системные метрики Xray
     */
    public SysMetrics getSysMetrics(String ipAddress, int grpcPort) {
        ManagedChannel channel = null;
        try {
            channel = ManagedChannelBuilder.forAddress(ipAddress, grpcPort)
                    .usePlaintext()
                    .build();

            StatsServiceGrpc.StatsServiceBlockingStub stub = StatsServiceGrpc.newBlockingStub(channel)
                    .withDeadlineAfter(3, TimeUnit.SECONDS);

            SysStatsResponse response = stub.getSysStats(SysStatsRequest.newBuilder().build());

            return SysMetrics.builder()
                    .uptime(response.getUptime())
                    .numGoroutine(response.getNumGoroutine())
                    .build();

        } catch (Exception e) {
            log.error("Failed to fetch sys stats from {}: {}", ipAddress, e.getMessage());
            return null;
        } finally {
            if (channel != null) channel.shutdown();
        }
    }

    /**
     * Получает суммарный исходящий трафик (Downlink) для всего сервера
     * Для этого опрашивается специальная метрика Xray
     */
    public long getServerTotalDownlink(String ipAddress, int grpcPort) {
        ManagedChannel channel = null;
        try {
            channel = ManagedChannelBuilder.forAddress(ipAddress, grpcPort)
                    .usePlaintext()
                    .build();

            StatsServiceGrpc.StatsServiceBlockingStub stub = StatsServiceGrpc.newBlockingStub(channel)
                    .withDeadlineAfter(3, TimeUnit.SECONDS);

            QueryStatsRequest request = QueryStatsRequest.newBuilder()
                    .setPattern("outbound>>>direct>>>traffic>>>downlink")
                    .setReset(false)
                    .build();

            QueryStatsResponse response = stub.queryStats(request);

            return response.getStatList().stream()
                    .mapToLong(Stat::getValue)
                    .sum();
        } catch (Exception e) {
            log.error("Failed to connect to Xray gRPC API at {}:{}. Error: {}", ipAddress, grpcPort, e.getMessage());
            return -1;
        } finally {
            if (channel != null && !channel.isShutdown()) {
                channel.shutdown();
            }
        }
    }

    /**
     * Проверка на работоспобность Xray (Health check)
     */
    public boolean isXrayAlive(String ipAddress, int grpcPort) {
        ManagedChannel channel = null;
        try {
            channel = ManagedChannelBuilder.forAddress(ipAddress, grpcPort)
                    .usePlaintext()
                    .build();

            StatsServiceGrpc.StatsServiceBlockingStub stub = StatsServiceGrpc.newBlockingStub(channel)
                    .withDeadlineAfter(2, TimeUnit.SECONDS);

            SysStatsResponse response = stub.getSysStats(SysStatsRequest.newBuilder().build());
            return response.getUptime() > 0;

        } catch (Exception e) {
            log.debug("Xray node {}:{} is DOWN", ipAddress, grpcPort);
            return false;
        } finally {
            if (channel != null && !channel.isShutdown()) {
                channel.shutdown();
            }
        }
    }
}
