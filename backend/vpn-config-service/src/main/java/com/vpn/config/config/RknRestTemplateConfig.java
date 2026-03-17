package com.vpn.config.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RknRestTemplateConfig {

    @Bean(name = "rknRestTemplate")
    public RestTemplate rknRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(15))
                .setReadTimeout(Duration.ofSeconds(60))
                .additionalInterceptors((request, body, execution) -> {
                    request.getHeaders().set(HttpHeaders.USER_AGENT,
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36");
                    request.getHeaders().set(HttpHeaders.ACCEPT, "application/json, text/plain, */*");
                    return execution.execute(request, body);
                })
                .build();
    }
}
