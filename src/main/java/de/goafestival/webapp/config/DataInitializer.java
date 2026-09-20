package de.goafestival.webapp.config;

import de.goafestival.webapp.domain.Band;
import de.goafestival.webapp.domain.Edition;
import de.goafestival.webapp.domain.FaqEntry;
import de.goafestival.webapp.domain.Location;
import de.goafestival.webapp.domain.SiteSettings;
import de.goafestival.webapp.repository.BandRepository;
import de.goafestival.webapp.repository.EditionRepository;
import de.goafestival.webapp.repository.FaqEntryRepository;
import de.goafestival.webapp.repository.LocationRepository;
import de.goafestival.webapp.repository.SiteSettingsRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Seeds a demo "2026" (current) and "2025" (archived) edition on first start,
 * so the site is showable without going through the admin UI first. Everything
 * here can be edited or deleted through /admin afterwards.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final EditionRepository editionRepository;
    private final BandRepository bandRepository;
    private final FaqEntryRepository faqEntryRepository;
    private final SiteSettingsRepository siteSettingsRepository;
    private final LocationRepository locationRepository;

    public DataInitializer(EditionRepository editionRepository, BandRepository bandRepository,
                            FaqEntryRepository faqEntryRepository, SiteSettingsRepository siteSettingsRepository,
                            LocationRepository locationRepository) {
        this.editionRepository = editionRepository;
        this.bandRepository = bandRepository;
        this.faqEntryRepository = faqEntryRepository;
        this.siteSettingsRepository = siteSettingsRepository;
        this.locationRepository = locationRepository;
    }

    @Override
    public void run(String... args) {
        if (editionRepository.count() > 0) {
            return;
        }

        Location tipsyApes = new Location();
        tipsyApes.setName("Tipsy Apes");
        tipsyApes.setStreet("Am Radeland 25");
        tipsyApes.setZipCity("21079 Hamburg");
        tipsyApes = locationRepository.save(tipsyApes);

        Edition edition2026 = new Edition();
        edition2026.setYear(2026);
        edition2026.setDisplayLabel("2026");
        edition2026.setTitle("Grumbrechtstraßen Open Air");
        edition2026.setStartDate(LocalDate.of(2026, 7, 10));
        edition2026.setEndDate(LocalDate.of(2026, 7, 11));
        edition2026.setLocation(tipsyApes);
        edition2026.setAboutText("Das Grumbrechtstraßen Open Air ist ein Indie Rock / Metal Festival von Schanien "
                + "Bands und Sherenernen über Bühnen bolten mobile.");
        edition2026.setColorPrimary("#2f6f68");
        edition2026.setColorSecondary("#e0559a");
        edition2026.setColorAccent("#f2c14e");
        edition2026.setCurrent(true);
        edition2026 = editionRepository.save(edition2026);

        addFaq(edition2026, 0, "Was ist das GOA ?", "Das GOA (Grumbrechtstraßen Open Air) ist ein privat organisiertes Open-Air-Festival mit lokalen Bands aus Hamburg und Umgebung.");
        addFaq(edition2026, 1, "Kann ich helfen?", "Ja! Schreib uns einfach über Instagram oder Facebook, wir freuen uns über jede helfende Hand beim Auf- und Abbau.");
        addFaq(edition2026, 2, "Wie funktioniert der Einlass?", "Der Einlass erfolgt an der Tageskasse vor Ort. Bitte bringt einen gültigen Ausweis mit.");
        addFaq(edition2026, 3, "Wie kann ich bezahlen?", "Vor Ort wird ausschließlich mit Bargeld bezahlt, Tickets könnt ihr im Vorfeld online oder an der Abendkasse erwerben.");
        addFaq(edition2026, 4, "Wie komme ich am Besten zum GOA?", "Am besten erreicht ihr uns mit dem Rad oder öffentlichen Verkehrsmitteln, Parkplätze in der Umgebung sind begrenzt.");

        addBand(edition2026, "Fiebertraum", "Psychedelic Noise Punk", "Hamburg",
                "„Kontrolliertes Chaos“ – „Wie eine Welle, die einen überrollt und in ihren Strömen mitreißt.“ "
                        + "Fiebertraum ist eine Band, die man live sehen muss, um das volle Ausmaß ihrer Musik zu erleben.",
                LocalDateTime.of(2026, 7, 10, 18, 0));
        addBand(edition2026, "Swap the Giant", "Indie Rock", "Hamburg",
                "Vier Freunde, eine Bühne, jede Menge Groove.", LocalDateTime.of(2026, 7, 10, 19, 30));
        addBand(edition2026, "Over the Under", "Southern/Groove Metal", "Norddeutschland",
                "Schwere Riffs treffen auf groovige Rhythmen.", LocalDateTime.of(2026, 7, 10, 21, 0));
        addBand(edition2026, "Shrooms", "Alternative Rock", "Hamburg",
                "Energiegeladener Alternative Rock direkt aus dem Herzen der Stadt.", LocalDateTime.of(2026, 7, 11, 17, 0));
        addBand(edition2026, "MICH", "Nu Metal", "Hamburg",
                "Frischer Nu-Metal-Sound mit viel Bühnenpräsenz.", LocalDateTime.of(2026, 7, 11, 18, 15));
        addBand(edition2026, "TurtleJet", "Space Rock", "Berlin",
                "Weite Klanglandschaften und treibende Bässe.", LocalDateTime.of(2026, 7, 11, 19, 30));
        addBand(edition2026, "Pranzled Gate", "Punk Rock", "Hamburg",
                "Schnell, laut, ehrlich.", LocalDateTime.of(2026, 7, 11, 20, 45));
        addBand(edition2026, "The Grey", "Hard Rock", "Hamburg",
                "Klassischer Hard Rock mit modernem Anstrich.", LocalDateTime.of(2026, 7, 11, 22, 0));

        Location grumbrechtstrasse = new Location();
        grumbrechtstrasse.setName("Grumbrechtstraße");
        grumbrechtstrasse.setStreet("Grumbrechtstraße 1");
        grumbrechtstrasse.setZipCity("21079 Hamburg");
        grumbrechtstrasse = locationRepository.save(grumbrechtstrasse);

        Edition edition2025 = new Edition();
        edition2025.setYear(2025);
        edition2025.setDisplayLabel("60");
        edition2025.setTitle("Grumbrechtstraßen Open Air");
        edition2025.setStartDate(LocalDate.of(2025, 7, 11));
        edition2025.setEndDate(LocalDate.of(2025, 7, 12));
        edition2025.setLocation(grumbrechtstrasse);
        edition2025.setAboutText("Die 60. Ausgabe des Grumbrechtstraßen Open Air – ein Rückblick.");
        edition2025.setColorPrimary("#2e5fa3");
        edition2025.setColorSecondary("#c9701f");
        edition2025.setColorAccent("#e07f22");
        edition2025.setCurrent(false);
        edition2025 = editionRepository.save(edition2025);

        addBand(edition2025, "Redestruction", "Heavy Rock", "Hamburg",
                "Kompromissloser Heavy Rock.", LocalDateTime.of(2025, 7, 11, 20, 0));
        addBand(edition2025, "Defcon Supernaut", "Stoner Rock / Grunge", "Hamburg",
                "Fuzz-Gitarren und 90er-Grunge-Vibes.", LocalDateTime.of(2025, 7, 11, 21, 15));
        addBand(edition2025, "Swap the Giant", "Indie Rock", "Hamburg",
                "Vier Freunde, eine Bühne, jede Menge Groove.", LocalDateTime.of(2025, 7, 12, 18, 0));
        addBand(edition2025, "Digital Moy", "Pop Rock Cover", "Hamburg",
                "Die bekanntesten Pop-Rock-Hits im eigenen Gewand.", LocalDateTime.of(2025, 7, 12, 19, 30));
        addBand(edition2025, "Mule Tales", "Alternative Rock", "Hamburg",
                "Erzählerischer Alternative Rock mit Herz.", LocalDateTime.of(2025, 7, 12, 21, 0));

        if (siteSettingsRepository.findById(SiteSettings.SINGLETON_ID).isEmpty()) {
            SiteSettings settings = new SiteSettings();
            settings.setImpressumText("<p>Grumbrechtstraßen Open Air<br/>Musterstraße 1<br/>21079 Hamburg</p>"
                    + "<p>Kontakt: info@goa-festival.de</p>");
            siteSettingsRepository.save(settings);
        }
    }

    private void addFaq(Edition edition, int order, String question, String answer) {
        FaqEntry entry = new FaqEntry();
        entry.setEdition(edition);
        entry.setSortOrder(order);
        entry.setQuestion(question);
        entry.setAnswer(answer);
        faqEntryRepository.save(entry);
    }

    private void addBand(Edition edition, String name, String genre, String herkunft, String description, LocalDateTime performanceAt) {
        Band band = new Band();
        band.setEdition(edition);
        band.setName(name);
        band.setGenre(genre);
        band.setHerkunft(herkunft);
        band.setDescription(description);
        band.setPerformanceAt(performanceAt);
        bandRepository.save(band);
    }
}
