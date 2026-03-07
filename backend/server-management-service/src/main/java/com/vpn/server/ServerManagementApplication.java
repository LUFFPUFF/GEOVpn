package com.vpn.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(
        scanBasePackages = {"com.vpn.server", "com.vpn.common"},
        exclude = {RedisRepositoriesAutoConfiguration.class}
)
public class ServerManagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServerManagementApplication.class, args);
    }
}
