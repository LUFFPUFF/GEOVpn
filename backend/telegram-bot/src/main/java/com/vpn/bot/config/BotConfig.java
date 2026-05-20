package com.vpn.bot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.telegram.telegrambots.bots.DefaultBotOptions;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class BotConfig {

    @Value("${telegram.bot.proxy.host}")
    private String proxyHost;

    @Value("${telegram.bot.proxy.port}")
    private int proxyPort;

    @Bean
    public DefaultBotOptions defaultBotOptions() {
        DefaultBotOptions options = new DefaultBotOptions();

        if (proxyHost != null && !proxyHost.isBlank() && proxyPort > 0) {
            options.setProxyHost(proxyHost);
            options.setProxyPort(proxyPort);
            options.setProxyType(DefaultBotOptions.ProxyType.SOCKS5);
        }

        return options;
    }

    @Bean(name = "botTaskExecutor")
    public Executor botTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("BotLogic-");
        executor.initialize();
        return executor;
    }
}
