package com.vpn.config.generator.shadowsocks;

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
 * Генератор Shadowsocks конфигураций
 *
 * Поддерживает:
 * - AEAD шифры (chacha20-ietf-poly1305, aes-256-gcm)
 * - Simple-obfs plugin (http/tls)
 * - SIP002/SIP003 URI формат
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShadowsocksConfigGenerator {

    @Value("${vpn.config.shadowsocks.port:8388}")
    private Integer shadowsocksPort;

    @Value("${vpn.config.shadowsocks.password:}")
    private String shadowsocksPassword;

    @Value("${vpn.config.shadowsocks.method:chacha20-ietf-poly1305}")
    private String shadowsocksMethod;

    @Value("${vpn.config.shadowsocks.plugin:obfs-local}")
    private String shadowsocksPlugin;

    @Value("${vpn.config.shadowsocks.plugin-opts:obfs=http;obfs-host=www.bing.com}")
    private String shadowsocksPluginOpts;

    /**
     * Генерирует Shadowsocks конфигурацию для устройства
     *
     * @param vpnConfig VPN конфигурация
     * @param server Сервер
     * @return SS конфигурация в виде Map
     */
    public Map<String, Object> generateConfig(VpnConfiguration vpnConfig, ServerDto server) {
        log.info("Генерация Shadowsocks конфига для device: {}", vpnConfig.getDeviceId());

        Map<String, Object> config = new HashMap<>();

        config.put("server", server.getIpAddress());
        config.put("server_port", shadowsocksPort);
        config.put("password", shadowsocksPassword);
        config.put("method", shadowsocksMethod);

        if (shadowsocksPlugin != null && !shadowsocksPlugin.isEmpty()) {
            config.put("plugin", shadowsocksPlugin);
            config.put("plugin_opts", shadowsocksPluginOpts);
        }

        config.put("local_address", "127.0.0.1");
        config.put("local_port", 1080);
        config.put("timeout", 300);
        config.put("fast_open", true);
        config.put("mode", "tcp_and_udp");

        log.debug("SS конфиг сгенерирован: server={}, port={}, method={}",
                server.getIpAddress(), shadowsocksPort, shadowsocksMethod);

        return config;
    }

    /**
     * Строит Shadowsocks URI (ss://)
     * Формат SIP002: ss://base64(method:password)@server:port/?plugin=xxx
     *
     * @param server Сервер
     * @return SS ссылка
     */
    public String buildShadowsocksLink(ServerDto server) {
        return buildShadowsocksLink(server, null);
    }

    /**
     * Строит Shadowsocks URI с именем
     *
     * @param server Сервер
     * @param name Имя конфигурации (опционально)
     * @return SS ссылка
     */
    public String buildShadowsocksLink(ServerDto server, String name) {
        log.debug("Построение SS ссылки для server: {}", server.getName());

        String userInfo = shadowsocksMethod + ":" + shadowsocksPassword;

        String encodedUserInfo = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(userInfo.getBytes());

        StringBuilder ssUri = new StringBuilder();
        ssUri.append("ss://")
                .append(encodedUserInfo)
                .append("@")
                .append(server.getIpAddress())
                .append(":")
                .append(shadowsocksPort);

        if (shadowsocksPlugin != null && !shadowsocksPlugin.isEmpty()) {
            ssUri.append("/?plugin=")
                    .append(encodePluginString());
        }

        if (name != null && !name.isEmpty()) {
            ssUri.append("#").append(urlEncode(name));
        } else {
            ssUri.append("#").append(urlEncode(server.getName() + "-SS"));
        }

        String link = ssUri.toString();

        log.debug("SS ссылка сгенерирована: {}",
                link.length() > 60 ? link.substring(0, 60) + "..." : link);

        return link;
    }

    private String encodePluginString() {
        if (shadowsocksPlugin == null || shadowsocksPlugin.isEmpty()) {
            return "";
        }

        String pluginStr = shadowsocksPlugin;

        if (shadowsocksPluginOpts != null && !shadowsocksPluginOpts.isEmpty()) {
            pluginStr += ";" + shadowsocksPluginOpts;
        }

        return urlEncode(pluginStr);
    }

    private String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8)
                    .replace("+", "%20");
        } catch (Exception e) {
            return value;
        }
    }

    public boolean isShadowsocksConfigured() {
        return shadowsocksPassword != null && !shadowsocksPassword.isEmpty();
    }

    public Map<String, Object> getShadowsocksInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("configured", isShadowsocksConfigured());
        info.put("port", shadowsocksPort);
        info.put("method", shadowsocksMethod);
        info.put("plugin", shadowsocksPlugin);
        return info;
    }
}
