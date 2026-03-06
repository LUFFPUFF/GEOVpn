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
        return String.format(
                "vless://%s@%s:%d?security=reality&sni=%s&fp=%s&pbk=%s&sid=%s&type=%s&flow=%s#%s",
                uuid,
                serverAddress,
                serverPort,
                sni,
                fingerprint,
                publicKey,
                shortId,
                networkType,
                flow,
                urlEncode(serverName)
        );
    }

    /**
     * URL encode для имени сервера
     */
    private String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
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
