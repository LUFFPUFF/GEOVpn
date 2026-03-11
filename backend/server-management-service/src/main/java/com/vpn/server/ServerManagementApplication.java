package com.vpn.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableFeignClients(basePackages = "com.vpn.server.grpc")
@SpringBootApplication(
        scanBasePackages = {"com.vpn.server", "com.vpn.common"},
        exclude = {RedisRepositoriesAutoConfiguration.class}
)
public class ServerManagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServerManagementApplication.class, args);
    }
}
