package de.goafestival.webapp.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/** Small formatting helpers used from Thymeleaf templates via the SpEL {@code T()} operator. */
public final class GermanDateFormats {

    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("EEEE d. MMMM yyyy", Locale.GERMAN);
    private static final DateTimeFormatter DATE_LABEL = DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.GERMAN);
    private static final DateTimeFormatter TIME_LABEL = DateTimeFormatter.ofPattern("HH:mm", Locale.GERMAN);
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN);
    private static final DateTimeFormatter MONTH_YEAR = DateTimeFormatter.ofPattern("MM.yyyy", Locale.GERMAN);

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

    /**
     * Short venue-announcement date label, e.g. "11.11.2026" for a single day,
     * "10.&amp;11.07.2026" for two consecutive days, "10.-12.07.2026" for three or
     * more. Falls back to two full dates when the span crosses a month or year.
     */
    public static String headlinerDateLabel(LocalDate start, LocalDate end) {
        if (start == null) {
            return "";
        }
        if (end == null || end.equals(start)) {
            return SHORT_DATE.format(start);
        }
        if (!start.getMonth().equals(end.getMonth()) || start.getYear() != end.getYear()) {
            return SHORT_DATE.format(start) + " - " + SHORT_DATE.format(end);
        }
        long dayCount = ChronoUnit.DAYS.between(start, end) + 1;
        String separator = dayCount == 2 ? ".&" : ".-";
        return String.format("%02d%s%02d.%s", start.getDayOfMonth(), separator, end.getDayOfMonth(), MONTH_YEAR.format(end));
    }
}
