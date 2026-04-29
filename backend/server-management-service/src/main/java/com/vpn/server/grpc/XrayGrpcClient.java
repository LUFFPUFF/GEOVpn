package com.vpn.server.grpc;

import com.vpn.server.grpc.generated.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
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

    public SysMetrics getSysMetrics(String ipAddress, int grpcPort) throws InterruptedException {
        ManagedChannel channel = null;
        try {
            channel = ManagedChannelBuilder.forAddress(ipAddress, grpcPort)
                    .usePlaintext()
                    .keepAliveTime(10, TimeUnit.SECONDS)
                    .build();

            StatsServiceGrpc.StatsServiceBlockingStub stub = StatsServiceGrpc.newBlockingStub(channel)
                    .withDeadlineAfter(10, TimeUnit.SECONDS);

            SysStatsResponse response = stub.getSysStats(SysStatsRequest.newBuilder().build());

            return SysMetrics.builder()
                    .uptime(response.getUptime())
                    .numGoroutine(response.getNumGoroutine())
                    .build();

        } catch (Exception e) {
            log.error("Failed to fetch sys stats from {}:{}: {}", ipAddress, grpcPort, e.getMessage());
            return null;
        } finally {
            if (channel != null) {
                channel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
            }
        }
    }

    public long getServerTotalDownlink(String ipAddress, int grpcPort) {
        ManagedChannel channel = null;
        try {
            channel = ManagedChannelBuilder.forAddress(ipAddress, grpcPort)
                    .usePlaintext()
                    .build();

            StatsServiceGrpc.StatsServiceBlockingStub stub = StatsServiceGrpc.newBlockingStub(channel)
                    .withDeadlineAfter(10, TimeUnit.SECONDS); // Увеличили до 10

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
        } finally {
            if (channel != null) channel.shutdown();
        }
    }

    public List<Stat> getAllStatistics(String ipAddress, int grpcPort) {
        if (grpcPort <= 0) grpcPort = 62789;

        ManagedChannel channel = ManagedChannelBuilder.forAddress(ipAddress, grpcPort)
                .usePlaintext()
                .build();
        try {
            StatsServiceGrpc.StatsServiceBlockingStub stub = StatsServiceGrpc.newBlockingStub(channel)
                    .withDeadlineAfter(15, TimeUnit.SECONDS);

            QueryStatsResponse response = stub.queryStats(QueryStatsRequest.newBuilder()
                    .setPattern("")
                    .setReset(false)
                    .build());

            return response.getStatList();
        } catch (Exception e) {
            log.error("Failed to query stats from {}:{}: {}", ipAddress, grpcPort, e.getMessage());
            return List.of();
        } finally {
            channel.shutdown();
        }
    }

    public boolean removeUser(String ipAddress, int grpcPort, String inboundTag, String email) {
        ManagedChannel channel = null;
        try {
            channel = ManagedChannelBuilder.forAddress(ipAddress, grpcPort)
                    .usePlaintext()
                    .build();

            HandlerServiceGrpc.HandlerServiceBlockingStub stub = HandlerServiceGrpc.newBlockingStub(channel)
                    .withDeadlineAfter(10, TimeUnit.SECONDS);

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
        } finally {
            if (channel != null) channel.shutdown();
        }
    }
}