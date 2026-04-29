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
 * Builder для построения VLESS ссылок.
 *
 * Поддерживаемые транспорты:
 *   - tcp  → vless://...?type=tcp&security=reality&flow=xtls-rprx-vision...
 *   - xhttp → vless://...?type=xhttp&path=...&mode=...&security=reality&flow=xtls-rprx-vision...
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VlessLinkBuilder {

    private final VpnConfigProperties configProperties;

    /**
     * Стандартная TCP VLESS-ссылка (для RU relay и legacy-серверов).
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
                .networkType("tcp")
                .flow(configProperties.getVless().getFlow())
                .serverName(serverName)
                .build();

        if (!vlessLink.isValid()) {
            throw new ValidationException("Invalid VLESS link parameters");
        }

        String link = vlessLink.toVlessUrl();
        log.info("Built TCP VLESS link for server: {}, uuid: {}...",
                serverName, uuid.toString().substring(0, 8));
        return link;
    }

    /**
     * Relay-ссылка (всегда TCP).
     */
    public String buildRelayVlessLink(
            UUID uuid,
            String email,
            String relayServerAddress,
            Integer relayServerPort,
            String relayPublicKey,
            String relayShortId,
            String serverName
    ) {
        VlessLink vlessLink = VlessLink.builder()
                .uuid(uuid)
                .email(email)
                .serverAddress(relayServerAddress)
                .serverPort(relayServerPort)
                .sni(configProperties.getVless().getRelaySni())
                .fingerprint(configProperties.getVless().getDefaultFingerprint())
                .publicKey(relayPublicKey)
                .shortId(relayShortId)
                .networkType("tcp")
                .flow(configProperties.getVless().getFlow())
                .serverName(serverName + " [RU-Антиглуш]")
                .build();

        log.info("Built RELAY VLESS link for server: {}, uuid: {}...",
                serverName, uuid.toString().substring(0, 8));
        return vlessLink.toVlessUrl();
    }

    /**
     * Custom-ссылка с явным указанием SNI и fingerprint.
     * Транспорт определяется по наличию xhttpParams:
     *   - xhttpParams == null → TCP
     *   - xhttpParams != null → XHTTP
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
        return buildVlessLinkCustom(uuid, serverAddress, serverPort, serverName,
                publicKey, shortId, customSni, customFingerprint, null);
    }

    /**
     * Custom-ссылка с поддержкой XHTTP.
     *
     * @param xhttpParams  если null — строится TCP-ссылка;
     *                     иначе — XHTTP с параметрами path/mode/padding
     */
    public String buildVlessLinkCustom(
            UUID uuid,
            ServerAddress serverAddress,
            Integer serverPort,
            String serverName,
            String publicKey,
            String shortId,
            String customSni,
            String customFingerprint,
            XhttpParams xhttpParams
    ) {
        validateInputs(uuid, serverAddress, serverPort, publicKey, shortId);

        String networkType = (xhttpParams != null) ? "xhttp" : "tcp";

        String flow = "xhttp".equalsIgnoreCase(networkType) ? null : configProperties.getVless().getFlow();

        VlessLink vlessLink = VlessLink.builder()
                .uuid(uuid)
                .serverAddress(serverAddress.getValue())
                .serverPort(serverPort)
                .sni(customSni != null ? customSni : configProperties.getVless().getDefaultSni())
                .fingerprint(customFingerprint != null ? customFingerprint
                        : configProperties.getVless().getDefaultFingerprint())
                .publicKey(publicKey)
                .shortId(shortId)
                .networkType(networkType)
                .flow(flow)
                .serverName(serverName)
                .build();

        if (!vlessLink.isValid()) {
            throw new ValidationException("Invalid VLESS link parameters");
        }

        String baseLink = vlessLink.toVlessUrl();

        if (xhttpParams != null) {
            baseLink = injectXhttpParams(baseLink, xhttpParams);
        }

        log.info("Built {} VLESS link for server: {}, uuid: {}...",
                networkType.toUpperCase(), serverName, uuid.toString().substring(0, 8));
        return baseLink;
    }

    /**
     * Вставляет XHTTP-параметры в уже сформированную ссылку.
     * toVlessUrl() уже добавил type=xhttp; здесь добавляем path/mode/extra.
     *
     * Итоговый формат:
     *   vless://uuid@host:port?type=xhttp&path=/api/v1/data&mode=stream-one
     *          &security=reality&pbk=...&fp=chrome&sni=...&sid=...
     *          &flow=xtls-rprx-vision#NAME
     */
    private String injectXhttpParams(String link, XhttpParams p) {
        if (link == null) return null;

        int hashIdx = link.indexOf('#');
        String basePart = hashIdx >= 0 ? link.substring(0, hashIdx) : link;
        String namePart = hashIdx >= 0 ? link.substring(hashIdx) : "";

        StringBuilder sb = new StringBuilder(basePart);

        if (p.getPath() != null && !p.getPath().isBlank()) {
            sb.append("&path=").append(urlEncode(p.getPath()));
        }

        sb.append("&host=");

        if (p.getMode() != null && !p.getMode().isBlank()) {
            sb.append("&mode=").append(urlEncode(p.getMode()));
        }

        sb.append(namePart);
        return sb.toString();
    }

    public VlessLink parseVlessLink(String vlessUrl) {
        if (vlessUrl == null || !vlessUrl.startsWith("vless://")) {
            throw new ValidationException("Invalid VLESS URL format");
        }
        try {
            String withoutPrefix = vlessUrl.substring(8);
            String[] parts = withoutPrefix.split("\\?");
            if (parts.length != 2) throw new ValidationException("Invalid VLESS URL structure");

            String[] addressPart = parts[0].split("@");
            if (addressPart.length != 2) throw new ValidationException("Invalid address format");

            UUID uuid = UUID.fromString(addressPart[0]);
            String[] serverPart = addressPart[1].split(":");
            String serverAddress = serverPart[0];
            Integer serverPort = Integer.parseInt(serverPart[1]);

            String[] paramsPart = parts[1].split("#");
            String serverName = paramsPart.length > 1
                    ? java.net.URLDecoder.decode(paramsPart[1], StandardCharsets.UTF_8)
                    : "Unknown";

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

    private void validateInputs(UUID uuid, ServerAddress serverAddress,
                                Integer serverPort, String publicKey, String shortId) {
        if (uuid == null)                                 throw new ValidationException("UUID cannot be null");
        if (serverAddress == null)                        throw new ValidationException("Server address cannot be null");
        if (serverPort == null || serverPort <= 0 || serverPort > 65535)
            throw new ValidationException("Invalid server port: " + serverPort);
        if (publicKey == null || publicKey.isEmpty())     throw new ValidationException("Public key cannot be empty");
        if (shortId  == null || shortId.isEmpty())        throw new ValidationException("Short ID cannot be empty");
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * Параметры XHTTP-транспорта для VLESS ссылки.
     */
    @lombok.Value
    @lombok.Builder
    public static class XhttpParams {
        String path;
        String mode;
        String paddingBytes;
    }
}