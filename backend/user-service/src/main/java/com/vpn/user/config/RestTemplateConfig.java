package com.vpn.user.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.Proxy;

@Configuration
public class RestTemplateConfig {

    @Value("${service.telegram.proxy.host:vpn-proxy-bridge}")
    private String proxyHost;

    @Value("${service.telegram.proxy.port:10808}")
    private int proxyPort;

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        if (proxyHost != null && !proxyHost.isBlank() && proxyPort > 0) {
            Proxy proxy = new Proxy(
                    Proxy.Type.SOCKS,
                    InetSocketAddress.createUnresolved(proxyHost, proxyPort)
            );
            factory.setProxy(proxy);
        }

        factory.setConnectTimeout(4000);
        factory.setReadTimeout(4000);

        return new RestTemplate(factory);
    }
}
