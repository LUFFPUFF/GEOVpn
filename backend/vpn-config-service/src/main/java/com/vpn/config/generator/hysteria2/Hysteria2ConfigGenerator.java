package com.vpn.config.generator.hysteria2;

import com.vpn.config.config.VpnConfigProperties;
import com.vpn.config.domain.entity.VpnConfiguration;
import com.vpn.common.dto.ServerDto;
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

    private final VpnConfigProperties properties;

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

        if (obfsPassword != null && !obfsPassword.isEmpty() && !"none".equalsIgnoreCase(obfsType) && !obfsType.isEmpty()) {
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
     * Строит Hysteria2 URI (Запасной вариант)
     */
    public String buildHysteria2Link(ServerDto server, String name) {
        var hy2 = properties.getHysteria2();

        StringBuilder uri = new StringBuilder();
        uri.append("hysteria2://");
        uri.append(urlEncode(hy2.getPassword()));
        uri.append("@");
        uri.append(server.getIpAddress()).append(":").append(hy2.getPort());

        uri.append("/?");

        uri.append("insecure=").append(hy2.getTls().isInsecure() ? "1" : "0");
        uri.append("&sni=").append(urlEncode(hy2.getSni()));

        if (hy2.getObfs() != null && hy2.getObfs().getPassword() != null &&
                !hy2.getObfs().getPassword().isEmpty() && !"none".equalsIgnoreCase(hy2.getObfs().getType())) {
            uri.append("&obfs=").append(urlEncode(hy2.getObfs().getType()));
            uri.append("&obfs-password=").append(urlEncode(hy2.getObfs().getPassword()));
        }

        uri.append("#").append(urlEncode(name != null ? name : "HY2"));

        return uri.toString();
    }

    public boolean isHysteria2Configured() {
        return properties.getHysteria2().isEnabled() &&
                properties.getHysteria2().getPassword() != null;
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