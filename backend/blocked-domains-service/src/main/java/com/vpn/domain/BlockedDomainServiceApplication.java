package com.vpn.domain;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;

@SpringBootApplication(
        scanBasePackages = {"com.vpn.domain", "com.vpn.common"},
        exclude = {RedisReactiveAutoConfiguration.class})
public class BlockedDomainServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BlockedDomainServiceApplication.class, args);
    }
}
