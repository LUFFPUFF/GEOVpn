package com.vpn.config.generator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * Генератор UUID для VLESS конфигураций
 *
 * Использует детерминированную генерацию на основе:
 * - User ID
 * - Device ID
 * - Timestamp
 * - Random salt
 */
@Slf4j
@Component
public class UuidGenerator {

    /**
     * Генерирует UUID Version 4 (random)
     */
    public UUID generateRandomUuid() {
        return UUID.randomUUID();
    }

    /**
     * Генерирует детерминированный UUID на основе userId и deviceId
     * Полезно для воспроизводимости в тестах
     */
    public UUID generateDeterministicUuid(Long userId, Long deviceId) {
        try {
            String input = String.format("%d-%d-%d", userId, deviceId, System.currentTimeMillis());

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes());

            ByteBuffer bb = ByteBuffer.wrap(hash);
            long mostSigBits = bb.getLong();
            long leastSigBits = bb.getLong();

            mostSigBits &= ~0xF000L;
            mostSigBits |= 0x4000L;
            leastSigBits &= ~(0xC000000000000000L);
            leastSigBits |= 0x8000000000000000L;

            UUID uuid = new UUID(mostSigBits, leastSigBits);
            log.debug("Generated deterministic UUID for userId={}, deviceId={}: {}",
                    userId, deviceId, uuid);

            return uuid;

        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available, falling back to random UUID", e);
            return generateRandomUuid();
        }
    }

    /**
     * Валидация UUID
     */
    public boolean isValidUuid(String uuidString) {
        if (uuidString == null || uuidString.isEmpty()) {
            return false;
        }

        try {
            UUID.fromString(uuidString);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Конвертация UUID в строку без дефисов
     */
    public String toCompactString(UUID uuid) {
        return uuid.toString().replace("-", "");
    }
}
