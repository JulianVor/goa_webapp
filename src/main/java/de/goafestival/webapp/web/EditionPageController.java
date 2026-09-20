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

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Renders the public festival page: the current edition at "/", any past
 * edition (the "Historie") at "/goa/{year}", and the Kneipenkonzerte
 * (small pub shows) list/detail pages.
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
        model.addAttribute("nextKneipenkonzert", editionService.findNextKneipenkonzert().orElse(null));
        return "edition";
    }

    @GetMapping("/goa/{year}")
    public String archivedEdition(@PathVariable int year, Model model) {
        Edition edition = editionService.getByYearOrThrow(year);
        populateModel(model, edition, edition.isCurrent());
        return "edition";
    }

    @GetMapping("/kneipenkonzerte")
    public String kneipenkonzerte(Model model) {
        Edition current = editionService.getCurrentOrThrow();
        List<Edition> all = editionService.findKneipenkonzerte();
        LocalDate today = LocalDate.now();

        List<Edition> upcoming = all.stream()
                .filter(e -> e.getStartDate() != null && !e.getStartDate().isBefore(today))
                .sorted(Comparator.comparing(Edition::getStartDate))
                .toList();
        List<Edition> past = all.stream()
                .filter(e -> e.getStartDate() == null || e.getStartDate().isBefore(today))
                .sorted(Comparator.comparing(Edition::getStartDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        model.addAttribute("edition", current);
        model.addAttribute("upcoming", upcoming);
        model.addAttribute("past", past);
        model.addAttribute("archivedEditions", editionService.findArchivedEditions());
        return "kneipenkonzerte-list";
    }

    @GetMapping("/kneipenkonzerte/{id}")
    public String kneipenkonzertDetail(@PathVariable Long id, Model model) {
        Edition edition = editionService.getKneipenkonzertByIdOrThrow(id);
        populateModel(model, edition, false);
        return "edition";
    }

    private void populateModel(Model model, Edition edition, boolean isCurrentView) {
        List<Band> bands = bandService.findByEdition(edition.getId());
        List<DayLineup> dayLineups = bandService.groupByDay(bands);

        model.addAttribute("edition", edition);
        model.addAttribute("dayLineups", dayLineups);
        model.addAttribute("faqEntries", faqEntryService.findByEdition(edition.getId()));
        model.addAttribute("isCurrentView", isCurrentView);
        model.addAttribute("archivedEditions", editionService.findArchivedEditions());
    }
}
