package com.vpn.server.service;

import com.vpn.server.grpc.XrayGrpcClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/telegram_vpn",
        "spring.datasource.username=admin",
        "spring.datasource.password=admin"
})
public class TrafficTest {

    @Autowired
    private XrayGrpcClient xrayGrpcClient;

    @Test
    void testRealTraffic() {
        String serverIp = "193.104.33.209";
        int grpcPort = 10085;
        String userEmail = "test_user";

        System.out.println("--- ПОДКЛЮЧАЕМСЯ К СЕРВЕРУ ---");

        boolean alive = xrayGrpcClient.isXrayAlive(serverIp, grpcPort);
        System.out.println("Статус сервера: " + (alive ? "ONLINE ✅" : "OFFLINE ❌"));

        if (alive) {
            long bytes = xrayGrpcClient.getUserTraffic(serverIp, grpcPort, userEmail);
            double megabytes = bytes / (1024.0 * 1024.0);

            System.out.println("Трафик пользователя: " + String.format("%.2f", megabytes) + " MB");
            System.out.println("В байтах: " + bytes);
        }
    }
}
