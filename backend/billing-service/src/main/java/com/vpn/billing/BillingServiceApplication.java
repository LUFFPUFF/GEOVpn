package com.vpn.billing;

import com.vpn.common.security.aspect.SecurityAspect;
import com.vpn.common.security.config.GlobalSecurityConfig;
import com.vpn.common.security.filter.SecurityFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@EnableFeignClients(basePackages = "com.vpn.billing.client")
@SpringBootApplication(
        scanBasePackages = {"com.vpn.billing", "com.vpn.common"},
        exclude = {RedisRepositoriesAutoConfiguration.class}
)
public class BillingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BillingServiceApplication.class, args);
    }
}
