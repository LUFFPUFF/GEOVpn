package com.vpn.bot.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.telegram.telegrambots.bots.DefaultBotOptions;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.Executor;

@Slf4j
@Configuration
@EnableAsync
public class BotConfig {

    private static final int PROXY_CHECK_TIMEOUT_MS = 3000;

    @Value("${telegram.bot.proxy.host:}")
    private String proxyHost;

    @Value("${telegram.bot.proxy.port:0}")
    private int proxyPort;

    @Bean
    public DefaultBotOptions defaultBotOptions() {
        DefaultBotOptions options = new DefaultBotOptions();

        if (isProxyConfigured() && isProxyReachable()) {
            options.setProxyHost(proxyHost);
            options.setProxyPort(proxyPort);
            options.setProxyType(DefaultBotOptions.ProxyType.SOCKS5);
            log.info("[BOT PROXY] Using SOCKS5 proxy {}:{}", proxyHost, proxyPort);
        } else if (isProxyConfigured()) {
            log.warn("[BOT PROXY] Proxy {}:{} unreachable — falling back to direct connection",
                    proxyHost, proxyPort);
        } else {
            log.info("[BOT PROXY] No proxy configured — using direct connection");
        }

        return options;
    }

    private boolean isProxyConfigured() {
        return proxyHost != null && !proxyHost.isBlank() && proxyPort > 0;
    }

    private boolean isProxyReachable() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(proxyHost, proxyPort), PROXY_CHECK_TIMEOUT_MS);
            log.debug("[BOT PROXY] Health check passed for {}:{}", proxyHost, proxyPort);
            return true;
        } catch (IOException e) {
            log.warn("[BOT PROXY] Health check failed for {}:{} — {}", proxyHost, proxyPort, e.getMessage());
            return false;
        }
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