package com.vpn.config.generator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RealityConfigGeneratorTest {

    private final RealityConfigGenerator generator = new RealityConfigGenerator();

    @Test
    @DisplayName("Генерация публичного ключ")
    void shouldGeneratePublicKey() {
        String publicKey = generator.generatePublicKey();

        assertThat(publicKey).isNotNull();
        assertThat(publicKey).isNotEmpty();
        assertThat(generator.isValidPublicKey(publicKey)).isTrue();
    }

    @Test
    @DisplayName("Генерация короткого идентификатора")
    void shouldGenerateShortId() {
        String shortId = generator.generateShortId();

        assertThat(shortId).isNotNull();
        assertThat(shortId).hasSize(16);
        assertThat(shortId).matches("^[0-9a-f]{16}$");
        assertThat(generator.isValidShortId(shortId)).isTrue();
    }

    @Test
    @DisplayName("Проверка публичного ключа")
    void shouldValidatePublicKeys() {
        String validKey = generator.generatePublicKey();
        String invalidKey = "invalid-key";

        assertThat(generator.isValidPublicKey(validKey)).isTrue();
        assertThat(generator.isValidPublicKey(invalidKey)).isFalse();
        assertThat(generator.isValidPublicKey(null)).isFalse();
        assertThat(generator.isValidPublicKey("")).isFalse();
    }

    @Test
    @DisplayName("Проверка коротких ID's")
    void shouldValidateShortIds() {
        String validId = generator.generateShortId();
        String invalidId = "not-hex";

        assertThat(generator.isValidShortId(validId)).isTrue();
        assertThat(generator.isValidShortId(invalidId)).isFalse();
        assertThat(generator.isValidShortId("123")).isFalse();
    }

    @Test
    @DisplayName("Проверка рекомендуемых SNI's")
    void shouldProvideRecommendedSnis() {
        String[] snis = generator.getRecommendedSnis();

        assertThat(snis).isNotEmpty();
        assertThat(snis).contains("www.google.com", "www.microsoft.com");
    }
}