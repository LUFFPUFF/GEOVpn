package com.vpn.common.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Утилиты для работы с датами и временем
 */
public final class DateUtils {

    private DateUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;
    private static final ZoneId UTC = ZoneId.of("UTC");

    /**
     * Получить текущее время в UTC
     */
    public static LocalDateTime nowUTC() {
        return LocalDateTime.now(UTC);
    }

    /**
     * Получить текущий Instant
     */
    public static Instant nowInstant() {
        return Instant.now();
    }

    /**
     * Проверить истекла ли дата
     */
    public static boolean isExpired(LocalDateTime dateTime) {
        return dateTime != null && dateTime.isBefore(nowUTC());
    }

    /**
     * Проверить истек ли Instant
     */
    public static boolean isExpired(Instant instant) {
        return instant != null && instant.isBefore(nowInstant());
    }

    /**
     * Добавить дни к текущей дате
     */
    public static LocalDateTime addDays(int days) {
        return nowUTC().plusDays(days);
    }

    /**
     * Добавить часы к текущей дате
     */
    public static LocalDateTime addHours(int hours) {
        return nowUTC().plusHours(hours);
    }

    /**
     * Получить количество миллисекунд между датами
     */
    public static long millisBetween(LocalDateTime start, LocalDateTime end) {
        return Duration.between(start, end).toMillis();
    }

    /**
     * Получить количество дней между датами
     */
    public static long daysBetween(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.DAYS.between(start, end);
    }

    /**
     * Форматировать в ISO 8601
     */
    public static String formatISO(LocalDateTime dateTime) {
        return dateTime.atZone(UTC).format(ISO_FORMATTER);
    }

    /**
     * Конвертировать LocalDateTime в Instant
     */
    public static Instant toInstant(LocalDateTime dateTime) {
        return dateTime.atZone(UTC).toInstant();
    }

    /**
     * Конвертировать Instant в LocalDateTime
     */
    public static LocalDateTime toLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, UTC);
    }

    /**
     * Проверить что дата в будущем
     */
    public static boolean isFuture(LocalDateTime dateTime) {
        return dateTime != null && dateTime.isAfter(nowUTC());
    }

    /**
     * Получить начало дня
     */
    public static LocalDateTime startOfDay(LocalDateTime dateTime) {
        return dateTime.truncatedTo(ChronoUnit.DAYS);
    }

    /**
     * Получить конец дня
     */
    public static LocalDateTime endOfDay(LocalDateTime dateTime) {
        return dateTime.truncatedTo(ChronoUnit.DAYS).plusDays(1).minusNanos(1);
    }
}