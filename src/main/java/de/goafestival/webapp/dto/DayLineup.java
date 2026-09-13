package de.goafestival.webapp.dto;

import de.goafestival.webapp.domain.Band;

import java.time.LocalDate;
import java.util.List;

/** One day of the line-up: its date, a display label ("Freitag 10. Juli 2026") and its bands. */
public record DayLineup(LocalDate date, String label, List<Band> bands) {
}
