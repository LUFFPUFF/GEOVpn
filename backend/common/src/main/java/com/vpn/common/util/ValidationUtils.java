package com.vpn.common.util;

import com.vpn.common.exception.ValidationException;

import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Утилиты для валидации данных
 */
public final class ValidationUtils {

    private ValidationUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    /**
     * Проверить что объект не null
     */
    public static void validateNotNull(Object obj, String fieldName) {
        if (obj == null) {
            throw new ValidationException(fieldName + " cannot be null");
        }
    }

    /**
     * Проверить что строка не пустая
     */
    public static void validateNotEmpty(String str, String fieldName) {
        if (StringUtils.isNullOrEmpty(str)) {
            throw new ValidationException(fieldName + " cannot be empty");
        }
    }

    /**
     * Проверить что коллекция не пустая
     */
    public static void validateNotEmpty(Collection<?> collection, String fieldName) {
        if (collection == null || collection.isEmpty()) {
            throw new ValidationException(fieldName + " cannot be empty");
        }
    }

    /**
     * Проверить UUID формат
     */
    public static void validateUUID(String uuid, String fieldName) {
        validateNotEmpty(uuid, fieldName);
        if (!UUID_PATTERN.matcher(uuid).matches()) {
            throw new ValidationException(fieldName + " must be valid UUID format");
        }
    }

    /**
     * Проверить что число положительное
     */
    public static void validatePositive(long number, String fieldName) {
        if (number <= 0) {
            throw new ValidationException(fieldName + " must be positive");
        }
    }

    /**
     * Проверить что число не отрицательное
     */
    public static void validateNonNegative(long number, String fieldName) {
        if (number < 0) {
            throw new ValidationException(fieldName + " cannot be negative");
        }
    }

    /**
     * Проверить Telegram ID
     */
    public static void validateTelegramId(Long telegramId) {
        validateNotNull(telegramId, "Telegram ID");
        validatePositive(telegramId, "Telegram ID");
    }

    /**
     * Проверить email формат
     */
    public static void validateEmail(String email) {
        validateNotEmpty(email, "Email");
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new ValidationException("Invalid email format");
        }
    }

    /**
     * Проверить диапазон
     */
    public static void validateRange(long value, long min, long max, String fieldName) {
        if (value < min || value > max) {
            throw new ValidationException(
                    String.format("%s must be between %d and %d", fieldName, min, max)
            );
        }
    }

    /**
     * Проверить длину строки
     */
    public static void validateLength(String str, int minLength, int maxLength, String fieldName) {
        validateNotEmpty(str, fieldName);
        int length = str.length();
        if (length < minLength || length > maxLength) {
            throw new ValidationException(
                    String.format("%s length must be between %d and %d characters",
                            fieldName, minLength, maxLength)
            );
        }
    }

    /**
     * Проверить что строка содержит только буквы и цифры
     */
    public static void validateAlphanumeric(String str, String fieldName) {
        validateNotEmpty(str, fieldName);
        if (!str.matches("^[a-zA-Z0-9]+$")) {
            throw new ValidationException(fieldName + " must contain only letters and numbers");
        }
    }
}