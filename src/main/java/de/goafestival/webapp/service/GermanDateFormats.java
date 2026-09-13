package de.goafestival.webapp.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Small formatting helpers used from Thymeleaf templates via the SpEL {@code T()} operator. */
public final class GermanDateFormats {

    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("EEEE d. MMMM yyyy", Locale.GERMAN);
    private static final DateTimeFormatter DATE_LABEL = DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.GERMAN);
    private static final DateTimeFormatter TIME_LABEL = DateTimeFormatter.ofPattern("HH:mm", Locale.GERMAN);

    private GermanDateFormats() {
    }

    public static String dayLabel(LocalDate date) {
        return date == null ? "" : DAY_LABEL.format(date);
    }

    public static String dateLabel(LocalDate date) {
        return date == null ? "" : DATE_LABEL.format(date);
    }

    public static String timeLabel(LocalDateTime dateTime) {
        return dateTime == null ? "" : TIME_LABEL.format(dateTime) + " Uhr";
    }

    public static String dayAndTimeLabel(LocalDateTime dateTime) {
        return dateTime == null ? "" : DAY_LABEL.format(dateTime) + ", " + TIME_LABEL.format(dateTime) + " Uhr";
    }
}
