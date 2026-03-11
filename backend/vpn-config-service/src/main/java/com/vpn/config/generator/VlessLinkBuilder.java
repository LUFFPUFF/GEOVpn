package com.vpn.config.generator;

import com.vpn.common.exception.ValidationException;
import com.vpn.config.config.VpnConfigProperties;
import com.vpn.config.domain.valueobject.ServerAddress;
import com.vpn.config.domain.valueobject.VlessLink;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Builder для построения VLESS ссылок
 *
 * Format:
 * vless://UUID@IP:PORT?security=reality&sni=SNI&fp=FINGERPRINT&pbk=PUBLIC_KEY&sid=SHORT_ID&type=tcp&flow=FLOW#NAME
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VlessLinkBuilder {

    private final VpnConfigProperties configProperties;

    /**
     * Построить VLESS ссылку из компонентов
     */
    public String buildVlessLink(
            UUID uuid,
            String email,
            ServerAddress serverAddress,
            Integer serverPort,
            String serverName,
            String publicKey,
            String shortId
    ) {

        validateInputs(uuid, serverAddress, serverPort, publicKey, shortId);

        VlessLink vlessLink = VlessLink.builder()
                .uuid(uuid)
                .email(email)
                .serverAddress(serverAddress.getValue())
                .serverPort(serverPort)
                .sni(configProperties.getVless().getDefaultSni())
                .fingerprint(configProperties.getVless().getDefaultFingerprint())
                .publicKey(publicKey)
                .shortId(shortId)
                .networkType(configProperties.getVless().getNetworkType())
                .flow(configProperties.getVless().getFlow())
                .serverName(serverName)
                .build();

        if (!vlessLink.isValid()) {
            throw new ValidationException("Invalid VLESS link parameters");
        }

        String link = vlessLink.toVlessUrl();

        log.info("Built VLESS link for server: {}, uuid: {}",
                serverName, uuid.toString().substring(0, 8) + "...");

        return link;

    }

    /**
     * Построить с custom SNI и fingerprint
     */
    public String buildVlessLinkCustom(
            UUID uuid,
            ServerAddress serverAddress,
            Integer serverPort,
            String serverName,
            String publicKey,
            String shortId,
            String customSni,
            String customFingerprint
    ) {

        validateInputs(uuid, serverAddress, serverPort, publicKey, shortId);

        VlessLink vlessLink = VlessLink.builder()
                .uuid(uuid)
                .serverAddress(serverAddress.getValue())
                .serverPort(serverPort)
                .sni(customSni != null ? customSni : configProperties.getVless().getDefaultSni())
                .fingerprint(customFingerprint != null ? customFingerprint : configProperties.getVless().getDefaultFingerprint())
                .publicKey(publicKey)
                .shortId(shortId)
                .networkType(configProperties.getVless().getNetworkType())
                .flow(configProperties.getVless().getFlow())
                .serverName(serverName)
                .build();

        if (!vlessLink.isValid()) {
            throw new ValidationException("Invalid VLESS link parameters");
        }

        return vlessLink.toVlessUrl();
    }

    /**
     * Парсинг VLESS ссылки обратно в компоненты
     */
    public VlessLink parseVlessLink(String vlessUrl) {
        if (vlessUrl == null || !vlessUrl.startsWith("vless://")) {
            throw new ValidationException("Invalid VLESS URL format");
        }

        try {
            String withoutPrefix = vlessUrl.substring(8);

            String[] parts = withoutPrefix.split("\\?");
            if (parts.length != 2) {
                throw new ValidationException("Invalid VLESS URL structure");
            }

            String[] addressPart = parts[0].split("@");
            if (addressPart.length != 2) {
                throw new ValidationException("Invalid address format");
            }

            UUID uuid = UUID.fromString(addressPart[0]);

            String[] serverPart = addressPart[1].split(":");
            String serverAddress = serverPart[0];
            Integer serverPort = Integer.parseInt(serverPart[1]);

            // Парсим параметры
            String[] paramsPart = parts[1].split("#");
            String params = paramsPart[0];
            String serverName = paramsPart.length > 1 ?
                    java.net.URLDecoder.decode(paramsPart[1], StandardCharsets.UTF_8) : "Unknown";

            // todo: Полный парсинг всех параметров

            return VlessLink.builder()
                    .uuid(uuid)
                    .serverAddress(serverAddress)
                    .serverPort(serverPort)
                    .serverName(serverName)
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse VLESS link", e);
            throw new ValidationException("Failed to parse VLESS link: " + e.getMessage());
        }
    }

    private void validateInputs(
            UUID uuid,
            ServerAddress serverAddress,
            Integer serverPort,
            String publicKey,
            String shortId) {

        if (uuid == null) {
            throw new ValidationException("UUID cannot be null");
        }

        if (serverAddress == null) {
            throw new ValidationException("Server address cannot be null");
        }

        if (serverPort == null || serverPort <= 0 || serverPort > 65535) {
            throw new ValidationException("Invalid server port: " + serverPort);
        }

        if (publicKey == null || publicKey.isEmpty()) {
            throw new ValidationException("Public key cannot be empty");
        }

        if (shortId == null || shortId.isEmpty()) {
            throw new ValidationException("Short ID cannot be empty");
        }
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
