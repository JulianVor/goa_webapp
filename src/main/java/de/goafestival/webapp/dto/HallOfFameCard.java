package de.goafestival.webapp.dto;

import de.goafestival.webapp.domain.Band;

/**
 * One shuffled Hall of Fame grid entry: the band plus its edition's id (for the card-back
 * image URL), kept alongside it explicitly instead of via {@code band.getEdition()} since
 * that's lazy and the page is rendered outside any transaction (open-in-view is off).
 */
public record HallOfFameCard(Band band, Long editionId) {
}
