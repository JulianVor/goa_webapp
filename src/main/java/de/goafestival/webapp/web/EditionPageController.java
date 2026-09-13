package de.goafestival.webapp.web;

import de.goafestival.webapp.domain.Band;
import de.goafestival.webapp.domain.Edition;
import de.goafestival.webapp.dto.DayLineup;
import de.goafestival.webapp.service.BandService;
import de.goafestival.webapp.service.EditionService;
import de.goafestival.webapp.service.FaqEntryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Optional;

/**
 * Renders the public festival page: the current edition at "/", and any past
 * edition (the "Historie") at "/goa/{year}".
 */
@Controller
public class EditionPageController {

    private final EditionService editionService;
    private final BandService bandService;
    private final FaqEntryService faqEntryService;

    public EditionPageController(EditionService editionService, BandService bandService, FaqEntryService faqEntryService) {
        this.editionService = editionService;
        this.bandService = bandService;
        this.faqEntryService = faqEntryService;
    }

    @GetMapping("/")
    public String home(Model model) {
        Edition current = editionService.getCurrentOrThrow();
        populateModel(model, current, true);
        return "edition";
    }

    @GetMapping("/goa/{year}")
    public String archivedEdition(@PathVariable int year, Model model) {
        Edition edition = editionService.getByYearOrThrow(year);
        populateModel(model, edition, edition.isCurrent());
        return "edition";
    }

    private void populateModel(Model model, Edition edition, boolean isCurrentView) {
        List<Band> bands = bandService.findByEdition(edition.getId());
        List<DayLineup> dayLineups = bandService.groupByDay(bands);

        List<Edition> all = editionService.findAllOrdered();
        Optional<Edition> previous = all.stream()
                .filter(e -> e.getYear() < edition.getYear())
                .findFirst();

        model.addAttribute("edition", edition);
        model.addAttribute("dayLineups", dayLineups);
        model.addAttribute("faqEntries", faqEntryService.findByEdition(edition.getId()));
        model.addAttribute("isCurrentView", isCurrentView);
        model.addAttribute("previousEdition", previous.orElse(null));
    }
}
