package de.goafestival.webapp.web;

import de.goafestival.webapp.domain.Band;
import de.goafestival.webapp.domain.Edition;
import de.goafestival.webapp.dto.DayLineup;
import de.goafestival.webapp.dto.HallOfFameCard;
import de.goafestival.webapp.service.BandService;
import de.goafestival.webapp.service.EditionService;
import de.goafestival.webapp.service.FaqEntryService;
import de.goafestival.webapp.service.sharecard.BandShareCardService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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
    private final BandShareCardService bandShareCardService;

    public EditionPageController(EditionService editionService, BandService bandService, FaqEntryService faqEntryService,
                                  BandShareCardService bandShareCardService) {
        this.editionService = editionService;
        this.bandService = bandService;
        this.faqEntryService = faqEntryService;
        this.bandShareCardService = bandShareCardService;
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

    /**
     * All bands that have ever played the Festival (not the Kneipenkonzerte), shuffled
     * into one grid like a mixed deck of trading cards - freshly reshuffled on every
     * visit. Which year a band belongs to is still just a click away via the Archiv, so
     * dropping the per-year grouping here doesn't lose that information, just this page's
     * own strict chronological order.
     */
    @GetMapping("/hall-of-fame")
    public String hallOfFame(Model model) {
        Edition current = editionService.getCurrentOrThrow();
        List<HallOfFameCard> cards = editionService.findFestivalEditions().stream()
                .flatMap(e -> bandService.findByEdition(e.getId()).stream()
                        .map(band -> new HallOfFameCard(band, e.getId())))
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(cards);

        model.addAttribute("edition", current);
        model.addAttribute("archivedEditions", editionService.findArchivedEditions());
        model.addAttribute("cards", cards);
        model.addAttribute("pageTitle", "Hall of Fame – " + current.getTitle());
        return "hall-of-fame";
    }

    /** The card "back" for one edition - same image for every band in it, see BandShareCardService. */
    @GetMapping("/editions/{id}/card-back.png")
    @ResponseBody
    public ResponseEntity<byte[]> editionCardBack(@PathVariable Long id) throws IOException {
        Edition edition = editionService.getByIdOrThrow(id);
        byte[] png = bandShareCardService.renderEditionBackCached(edition);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .body(png);
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
