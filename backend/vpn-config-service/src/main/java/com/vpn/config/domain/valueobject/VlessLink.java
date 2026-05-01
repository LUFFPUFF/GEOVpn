package com.vpn.config.domain.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Value Object для VLESS ссылки
 * Immutable представление VLESS конфигурации
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VlessLink {

    private UUID uuid;
    private String email;
    private String serverAddress;
    private Integer serverPort;
    private String sni;
    private String fingerprint;
    private String publicKey;
    private String shortId;
    private String networkType;
    private String flow;
    private String serverName;

    /**
     * Построить полную VLESS ссылку
     */
    public String toVlessUrl() {
        StringBuilder sb = new StringBuilder();

        sb.append("vless://").append(uuid)
                .append("@").append(serverAddress)
                .append(":").append(serverPort)
                .append("?type=").append(networkType)
                .append("&encryption=none")
                .append("&security=reality")
                .append("&pbk=").append(publicKey)
                .append("&fp=").append(fingerprint)
                .append("&sni=").append(sni)
                .append("&sid=").append(shortId)
                .append("&spx=%2F");

        if (flow != null && !flow.isBlank() && !"null".equalsIgnoreCase(flow)) {
            sb.append("&flow=").append(flow);
        }

        sb.append("#").append(urlEncode(serverName));

        return sb.toString();
    }

    /**
     * URL encode для имени сервера
     */
    private String urlEncode(String value) {
        if (value == null) return "";
        try {
            return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8)
                    .replace("+", "%20");
        } catch (Exception e) {
            return value;
        }
    }

    /**
     * Валидация обязательных полей
     */
    public boolean isValid() {
        return uuid != null
                && serverAddress != null && !serverAddress.isEmpty()
                && serverPort != null && serverPort > 0 && serverPort <= 65535
                && sni != null && !sni.isEmpty()
                && publicKey != null && !publicKey.isEmpty()
                && shortId != null && !shortId.isEmpty();
    }
}
