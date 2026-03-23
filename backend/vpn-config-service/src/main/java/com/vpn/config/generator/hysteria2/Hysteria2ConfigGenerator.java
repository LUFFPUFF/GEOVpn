package com.vpn.config.generator.hysteria2;

import com.vpn.common.dto.ServerDto;
import com.vpn.config.domain.entity.VpnConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Генератор Hysteria2 конфигураций
 *
 * Hysteria2 - UDP-based протокол оптимизированный для:
 * - Мобильных сетей (LTE/5G)
 * - Плохих каналов связи
 * - Обхода TCP RST injection
 * - Обхода DPI
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Hysteria2ConfigGenerator {

    @Value("${vpn.config.hysteria2.port}")
    private Integer hysteria2Port;

    @Value("${vpn.config.hysteria2.password}")
    private String hysteria2Password;

    @Value("${vpn.config.hysteria2.obfs.type}")
    private String obfsType;

    @Value("${vpn.config.hysteria2.obfs.password}")
    private String obfsPassword;

    @Value("${vpn.config.hysteria2.sni}")
    private String sni;

    @Value("${vpn.config.hysteria2.tls.insecure}")
    private Boolean tlsInsecure;

    /**
     * Генерирует Hysteria2 конфигурацию
     */
    public Map<String, Object> generateConfig(VpnConfiguration vpnConfig, ServerDto server) {
        log.info("Генерация Hysteria2 конфига для device: {}", vpnConfig.getDeviceId());

        Map<String, Object> config = new HashMap<>();

        config.put("server", server.getIpAddress() + ":" + hysteria2Port);

        Map<String, String> auth = new HashMap<>();
        auth.put("type", "password");
        auth.put("password", hysteria2Password);
        config.put("auth", auth);

        Map<String, Object> tls = new HashMap<>();
        tls.put("sni", sni);
        tls.put("insecure", tlsInsecure);
        config.put("tls", tls);

        if (obfsPassword != null && !obfsPassword.isEmpty()) {
            Map<String, Object> obfs = new HashMap<>();
            obfs.put("type", obfsType);

            if ("salamander".equals(obfsType)) {
                Map<String, String> salamander = new HashMap<>();
                salamander.put("password", obfsPassword);
                obfs.put("salamander", salamander);
            }

            config.put("obfs", obfs);
        }

        config.put("transport", Map.of("type", "udp"));

        Map<String, Object> bandwidth = new HashMap<>();
        bandwidth.put("up", "100 mbps");
        bandwidth.put("down", "100 mbps");
        config.put("bandwidth", bandwidth);

        config.put("fastOpen", true);

        config.put("lazy", false);

        log.debug("Hysteria2 конфиг сгенерирован");

        return config;
    }

    /**
     * Строит Hysteria2 URI
     */
    public String buildHysteria2Link(ServerDto server, String name) {
        StringBuilder uri = new StringBuilder();
        uri.append("hysteria2://");
        uri.append(urlEncode(hysteria2Password));
        uri.append("@");
        uri.append(server.getIpAddress()).append(":").append(hysteria2Port);
        uri.append("?");

        if (obfsPassword != null && !obfsPassword.isEmpty()) {
            uri.append("obfs=").append(obfsType);
            uri.append("&obfs-password=").append(urlEncode(obfsPassword));
        }

        uri.append("&sni=").append(sni);

        if (tlsInsecure) {
            uri.append("&insecure=1");
        }

        uri.append("#").append(urlEncode(name != null ? name : "LTE-Hysteria2"));
        return uri.toString();
    }

    public boolean isHysteria2Configured() {
        return hysteria2Password != null && !hysteria2Password.isEmpty();
    }

    public Map<String, Object> getHysteria2Info() {
        Map<String, Object> info = new HashMap<>();
        info.put("configured", isHysteria2Configured());
        info.put("port", hysteria2Port);
        info.put("obfsType", obfsType);
        info.put("sni", sni);
        return info;
    }

    private String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8)
                    .replace("+", "%20");
        } catch (Exception e) {
            return value;
        }
    }
}
