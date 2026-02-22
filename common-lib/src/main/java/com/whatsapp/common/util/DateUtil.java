package com.whatsapp.common.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Date Utility Class
 *
 * Common date/time operations used across microservices.
 * All dates are handled in UTC internally and converted to user timezone when needed.
 *
 * @author WhatsApp Clone Team
 */
public final class DateUtil {

    private DateUtil() {
        // Prevent instantiation
    }

    // Common formatters
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Get current timestamp (UTC)
     */
    public static Instant now() {
        return Instant.now();
    }

    /**
     * Get current date (UTC)
     */
    public static LocalDate today() {
        return LocalDate.now(ZoneOffset.UTC);
    }

    /**
     * Get current datetime (UTC)
     */
    public static LocalDateTime nowDateTime() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    /**
     * Format Instant to ISO string
     */
    public static String formatISO(Instant instant) {
        if (instant == null) return null;
        return ISO_FORMATTER.format(instant);
    }

    /**
     * Parse ISO string to Instant
     */
    public static Instant parseISO(String isoString) {
        if (isoString == null || isoString.isBlank()) return null;
        return Instant.parse(isoString);
    }

    /**
     * Format date to string (yyyy-MM-dd)
     */
    public static String formatDate(LocalDate date) {
        if (date == null) return null;
        return DATE_FORMATTER.format(date);
    }

    /**
     * Format datetime to string (yyyy-MM-dd HH:mm:ss)
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return DATETIME_FORMATTER.format(dateTime);
    }

    /**
     * Check if instant is in the past
     */
    public static boolean isPast(Instant instant) {
        return instant != null && instant.isBefore(Instant.now());
    }

    /**
     * Check if instant is in the future
     */
    public static boolean isFuture(Instant instant) {
        return instant != null && instant.isAfter(Instant.now());
    }

    /**
     * Get difference in days between two instants
     */
    public static long daysBetween(Instant start, Instant end) {
        if (start == null || end == null) return 0;
        return ChronoUnit.DAYS.between(start, end);
    }

    /**
     * Get difference in hours between two instants
     */
    public static long hoursBetween(Instant start, Instant end) {
        if (start == null || end == null) return 0;
        return ChronoUnit.HOURS.between(start, end);
    }

    /**
     * Get difference in minutes between two instants
     */
    public static long minutesBetween(Instant start, Instant end) {
        if (start == null || end == null) return 0;
        return ChronoUnit.MINUTES.between(start, end);
    }

    /**
     * Add days to instant
     */
    public static Instant addDays(Instant instant, long days) {
        if (instant == null) return null;
        return instant.plus(days, ChronoUnit.DAYS);
    }

    /**
     * Add hours to instant
     */
    public static Instant addHours(Instant instant, long hours) {
        if (instant == null) return null;
        return instant.plus(hours, ChronoUnit.HOURS);
    }

    /**
     * Add minutes to instant
     */
    public static Instant addMinutes(Instant instant, long minutes) {
        if (instant == null) return null;
        return instant.plus(minutes, ChronoUnit.MINUTES);
    }

    /**
     * Convert Instant to LocalDateTime in specific timezone
     */
    public static LocalDateTime toLocalDateTime(Instant instant, ZoneId zoneId) {
        if (instant == null) return null;
        return LocalDateTime.ofInstant(instant, zoneId);
    }

    /**
     * Convert Instant to LocalDateTime in UTC
     */
    public static LocalDateTime toLocalDateTimeUTC(Instant instant) {
        return toLocalDateTime(instant, ZoneOffset.UTC);
    }

    /**
     * Start of day (00:00:00)
     */
    public static Instant startOfDay(LocalDate date) {
        if (date == null) return null;
        return date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    /**
     * End of day (23:59:59)
     */
    public static Instant endOfDay(LocalDate date) {
        if (date == null) return null;
        return date.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);
    }

    /**
     * Check if two instants are on the same day (UTC)
     */
    public static boolean isSameDay(Instant instant1, Instant instant2) {
        if (instant1 == null || instant2 == null) return false;
        LocalDate date1 = toLocalDateTimeUTC(instant1).toLocalDate();
        LocalDate date2 = toLocalDateTimeUTC(instant2).toLocalDate();
        return date1.equals(date2);
    }
}