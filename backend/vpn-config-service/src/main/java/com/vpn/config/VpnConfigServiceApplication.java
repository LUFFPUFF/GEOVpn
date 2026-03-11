package com.vpn.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"com.vpn.config", "com.vpn.common"})
@EnableFeignClients(basePackages = {"com.vpn.config", "com.vpn.common"})
@EnableCaching
public class VpnConfigServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(VpnConfigServiceApplication.class, args);
    }
}
