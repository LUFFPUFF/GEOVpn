package com.vpn.server.grpc;

import com.vpn.server.grpc.generated.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class XrayGrpcClient {

    private final Map<String, ManagedChannel> channelCache = new ConcurrentHashMap<>();

    @Data
    @Builder
    public static class SysMetrics {
        private long uptime;
        private int numGoroutine;
    }

    /**
     * Возвращает существующий открытый gRPC канал или создает новый, если его еще нет в кэше.
     */
    private ManagedChannel getOrCreateChannel(String ipAddress, int grpcPort) {
        String key = ipAddress + ":" + grpcPort;
        return channelCache.compute(key, (k, existingChannel) -> {
            if (existingChannel != null && !existingChannel.isShutdown() && !existingChannel.isTerminated()) {
                return existingChannel;
            }
            log.info("Creating new long-lived gRPC channel for {}:{}", ipAddress, grpcPort);
            return ManagedChannelBuilder.forAddress(ipAddress, grpcPort)
                    .usePlaintext()
                    .keepAliveTime(30, TimeUnit.SECONDS) // Пинг соединения для удержания открытым
                    .keepAliveTimeout(5, TimeUnit.SECONDS)
                    .keepAliveWithoutCalls(true)
                    .build();
        });
    }

    public SysMetrics getSysMetrics(String ipAddress, int grpcPort) {
        try {
            ManagedChannel channel = getOrCreateChannel(ipAddress, grpcPort);

            StatsServiceGrpc.StatsServiceBlockingStub stub = StatsServiceGrpc.newBlockingStub(channel)
                    .withDeadlineAfter(5, TimeUnit.SECONDS);

            SysStatsResponse response = stub.getSysStats(SysStatsRequest.newBuilder().build());

            return SysMetrics.builder()
                    .uptime(response.getUptime())
                    .numGoroutine(response.getNumGoroutine())
                    .build();

        } catch (Exception e) {
            log.error("Failed to fetch sys stats from {}:{}: {}", ipAddress, grpcPort, e.getMessage());
            return null;
        }
    }

    public long getServerTotalDownlink(String ipAddress, int grpcPort) {
        try {
            ManagedChannel channel = getOrCreateChannel(ipAddress, grpcPort);

            StatsServiceGrpc.StatsServiceBlockingStub stub = StatsServiceGrpc.newBlockingStub(channel)
                    .withDeadlineAfter(5, TimeUnit.SECONDS);

            QueryStatsRequest request = QueryStatsRequest.newBuilder()
                    .setPattern("outbound>>>direct>>>traffic>>>downlink")
                    .setReset(false)
                    .build();

            QueryStatsResponse response = stub.queryStats(request);

            return response.getStatList().stream()
                    .mapToLong(Stat::getValue)
                    .sum();
        } catch (Exception e) {
            log.error("Failed to get total downlink from {}:{}: {}", ipAddress, grpcPort, e.getMessage());
            return -1;
        }
    }

    public List<Stat> getAllStatistics(String ipAddress, int grpcPort) {
        if (grpcPort <= 0) grpcPort = 62789;

        try {
            ManagedChannel channel = getOrCreateChannel(ipAddress, grpcPort);

            StatsServiceGrpc.StatsServiceBlockingStub stub = StatsServiceGrpc.newBlockingStub(channel)
                    .withDeadlineAfter(10, TimeUnit.SECONDS);

            QueryStatsResponse response = stub.queryStats(QueryStatsRequest.newBuilder()
                    .setPattern("")
                    .setReset(false)
                    .build());

            return response.getStatList();
        } catch (Exception e) {
            log.error("Failed to query stats from {}:{}: {}", ipAddress, grpcPort, e.getMessage());
            return List.of();
        }
    }

    public boolean removeUser(String ipAddress, int grpcPort, String inboundTag, String email) {
        try {
            ManagedChannel channel = getOrCreateChannel(ipAddress, grpcPort);

            HandlerServiceGrpc.HandlerServiceBlockingStub stub = HandlerServiceGrpc.newBlockingStub(channel)
                    .withDeadlineAfter(5, TimeUnit.SECONDS);

            RemoveClientRequest request = RemoveClientRequest.newBuilder()
                    .setInboundTag(inboundTag)
                    .setEmail(email)
                    .build();

            stub.removeClient(request);
            log.info("Successfully removed user {} from server {}", email, ipAddress);
            return true;
        } catch (Exception e) {
            log.error("Failed to remove user {} from {}: {}", email, ipAddress, e.getMessage());
            return false;
        }
    }

    @jakarta.annotation.PreDestroy
    public void shutdownAllChannels() {
        log.info("Closing all cached gRPC channels...");
        channelCache.forEach((key, channel) -> {
            try {
                channel.shutdown().awaitTermination(2, TimeUnit.SECONDS);
                log.info("gRPC Channel for {} successfully closed.", key);
            } catch (Exception e) {
                log.warn("Failed to close gRPC channel for {}: {}", key, e.getMessage());
            }
        });
        channelCache.clear();
    }
}