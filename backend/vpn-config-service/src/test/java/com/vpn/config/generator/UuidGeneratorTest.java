package com.vpn.config.generator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UuidGeneratorTest {

    private final UuidGenerator uuidGenerator = new UuidGenerator();

    @Test
    @DisplayName("Проверка генерации случайного UUID")
    void shouldGenerateRandomUuid() {
        UUID uuid = uuidGenerator.generateRandomUuid();

        assertThat(uuid).isNotNull();
        assertThat(uuid.version()).isEqualTo(4);
    }

    @Test
    @DisplayName("Для одинаковых входных данных следует генерировать детерминированный UUID")
    void shouldGenerateDeterministicUuid() {
        Long userId = 123L;
        Long deviceId = 456L;

        UUID uuid1 = uuidGenerator.generateDeterministicUuid(userId, deviceId);

        try { Thread.sleep(10); } catch (InterruptedException ignored) {}

        UUID uuid2 = uuidGenerator.generateDeterministicUuid(userId, deviceId);

        assertThat(uuid1).isNotEqualTo(uuid2);

        assertThat(uuid1.version()).isEqualTo(4);
        assertThat(uuid2.version()).isEqualTo(4);
    }

    @Test
    @DisplayName("Строки UUID должны корректно проверяться")
    void shouldValidateUuidStrings() {
        String validUuid = "550e8400-e29b-41d4-a716-446655440000";
        String invalidUuid = "not-a-uuid";

        assertThat(uuidGenerator.isValidUuid(validUuid)).isTrue();
        assertThat(uuidGenerator.isValidUuid(invalidUuid)).isFalse();
        assertThat(uuidGenerator.isValidUuid(null)).isFalse();
        assertThat(uuidGenerator.isValidUuid("")).isFalse();
    }

    @Test
    @DisplayName("Необходимо преобразовать UUID в компактную строку")
    void shouldConvertToCompactString() {
        UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        String compact = uuidGenerator.toCompactString(uuid);

        assertThat(compact).isEqualTo("550e8400e29b41d4a716446655440000");
        assertThat(compact).doesNotContain("-");
    }
}