package de.goafestival.webapp.dto;

import de.goafestival.webapp.domain.Band;
import de.goafestival.webapp.domain.Edition;

import java.util.List;

/** One edition's section on the Hall of Fame page: the edition itself and its bands. */
public record EditionLineup(Edition edition, List<Band> bands) {
}
