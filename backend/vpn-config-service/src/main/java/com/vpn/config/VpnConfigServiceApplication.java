package com.vpn.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;

@SpringBootApplication(exclude = {RedisRepositoriesAutoConfiguration.class})
public class VpnConfigServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(VpnConfigServiceApplication.class, args);
    }
}
