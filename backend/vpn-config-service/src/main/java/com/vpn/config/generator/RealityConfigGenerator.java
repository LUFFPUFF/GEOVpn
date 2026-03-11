package com.vpn.config.generator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Генератор параметров для Reality protocol
 */

@Slf4j
@Component
public class RealityConfigGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int PUBLIC_KEY_LENGTH = 32;
    private static final int SHORT_ID_LENGTH = 8;

    /**
     * Генерирует X25519 public key (Base64)
     * todo В production это должен быть реальный ключ с сервера
     */
    public String generatePublicKey() {
        byte[] keyBytes = new byte[PUBLIC_KEY_LENGTH];
        SECURE_RANDOM.nextBytes(keyBytes);

        String publicKey = Base64.getEncoder().encodeToString(keyBytes);
        log.debug("Generated Reality public key: {}", publicKey.substring(0, 10) + "...");

        return publicKey;
    }

    /**
     * Генерирует Short ID (hex string)
     */
    public String generateShortId() {
        byte[] idBytes = new byte[SHORT_ID_LENGTH];
        SECURE_RANDOM.nextBytes(idBytes);

        StringBuilder hexString = new StringBuilder();
        for (byte b : idBytes) {
            hexString.append(String.format("%02x", b));
        }

        String shortId = hexString.toString();
        log.debug("Generated Short ID: {}", shortId);

        return shortId;
    }

    /**
     * Валидация public key
     */
    public boolean isValidPublicKey(String publicKey) {
        if (publicKey == null || publicKey.isEmpty()) {
            return false;
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(publicKey);
            return decoded.length == PUBLIC_KEY_LENGTH;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Валидация short ID
     */
    public boolean isValidShortId(String shortId) {
        if (shortId == null) {
            return false;
        }

        return shortId.matches("^[0-9a-fA-F]{16}$");
    }

    //todo доработать рекомендунмые маскировки
    /**
     * Получить рекомендуемые SNI для маскировки
     */
    public String[] getRecommendedSnis() {
        return new String[]{
                "www.google.com",
                "www.microsoft.com",
                "www.apple.com",
                "www.cloudflare.com",
                "www.amazon.com",
                "www.bing.com"
        };
    }

    /**
     * Получить рекомендуемые fingerprints
     */
    public String[] getRecommendedFingerprints() {
        return new String[]{
                "chrome",
                "firefox",
                "safari",
                "edge",
                "360",
                "qq"
        };
    }

}
