package de.goafestival.webapp.service.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.goafestival.webapp.domain.Band;
import de.goafestival.webapp.domain.Edition;
import de.goafestival.webapp.domain.FaqEntry;
import de.goafestival.webapp.domain.Location;
import de.goafestival.webapp.domain.NewsletterSubscriber;
import de.goafestival.webapp.domain.SiteSettings;
import de.goafestival.webapp.repository.BandRepository;
import de.goafestival.webapp.repository.EditionRepository;
import de.goafestival.webapp.repository.FaqEntryRepository;
import de.goafestival.webapp.repository.LocationRepository;
import de.goafestival.webapp.repository.NewsletterSubscriberRepository;
import de.goafestival.webapp.repository.SiteSettingsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Exports every DB row and every uploaded image into a single .zip (one
 * "data.json" plus the whole uploads/ tree), and restores that same .zip back -
 * meant for moving the whole site's content between deployments (e.g. production
 * to a test instance, see SiteLockFilter) or as a manual backup.
 *
 * <p>Import is a full replace, not a merge: it wipes every row and every stored
 * image and inserts exactly what the .zip holds, ids included. Ids are preserved
 * (via plain JDBC, bypassing Hibernate's identity generation) so links that
 * embed one - band and Kneipenkonzert detail pages - keep working after a
 * restore.
 */
@Service
public class DataExportImportService {

    private final EditionRepository editionRepository;
    private final BandRepository bandRepository;
    private final FaqEntryRepository faqEntryRepository;
    private final LocationRepository locationRepository;
    private final NewsletterSubscriberRepository newsletterSubscriberRepository;
    private final SiteSettingsRepository siteSettingsRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final Path uploadRoot;
    private final TransactionTemplate transactionTemplate;

    public DataExportImportService(EditionRepository editionRepository, BandRepository bandRepository,
            FaqEntryRepository faqEntryRepository, LocationRepository locationRepository,
            NewsletterSubscriberRepository newsletterSubscriberRepository,
            SiteSettingsRepository siteSettingsRepository, JdbcTemplate jdbcTemplate, ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager, @Value("${app.upload-dir:uploads}") String uploadDir) {
        this.editionRepository = editionRepository;
        this.bandRepository = bandRepository;
        this.faqEntryRepository = faqEntryRepository;
        this.locationRepository = locationRepository;
        this.newsletterSubscriberRepository = newsletterSubscriberRepository;
        this.siteSettingsRepository = siteSettingsRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    // ---------------------------------------------------------------- export

    @Transactional(readOnly = true)
    public void exportTo(OutputStream out) throws IOException {
        ExportData data = buildExportData();
        // objectMapper.writeValue(OutputStream, ...) closes its target stream when done
        // (Jackson's default AUTO_CLOSE_TARGET) - fatal here, since that would close the
        // whole zip after the first entry. Serializing to bytes first sidesteps that.
        byte[] json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(data);
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("data.json"));
            zip.write(json);
            zip.closeEntry();
            addUploadsToZip(zip);
        }
    }

    private ExportData buildExportData() {
        ExportData data = new ExportData();
        data.exportedAt = LocalDateTime.now().toString();

        for (Location location : locationRepository.findAllByOrderByNameAsc()) {
            ExportData.LocationDto dto = new ExportData.LocationDto();
            dto.id = location.getId();
            dto.name = location.getName();
            dto.street = location.getStreet();
            dto.zipCity = location.getZipCity();
            data.locations.add(dto);
        }

        for (Edition edition : editionRepository.findAll()) {
            ExportData.EditionDto dto = new ExportData.EditionDto();
            dto.id = edition.getId();
            dto.type = edition.getType().name();
            dto.year = edition.getYear();
            dto.displayLabel = edition.getDisplayLabel();
            dto.title = edition.getTitle();
            dto.startDate = edition.getStartDate();
            dto.endDate = edition.getEndDate();
            dto.locationId = edition.getLocation() != null ? edition.getLocation().getId() : null;
            dto.aboutText = edition.getAboutText();
            dto.colorPrimary = edition.getColorPrimary();
            dto.colorSecondary = edition.getColorSecondary();
            dto.colorAccent = edition.getColorAccent();
            dto.logoImagePath = edition.getLogoImagePath();
            dto.backgroundImagePath = edition.getBackgroundImagePath();
            dto.locationImagePath = edition.getLocationImagePath();
            data.editions.add(dto);
        }
        // The show* getters apply archived-edition fallback logic on top of the stored
        // value - re-derive the *stored* flags from scratch instead of trusting the loop
        // above (kept intentionally simple - see fillStoredToggles).
        fillStoredToggles(data);

        for (Band band : bandRepository.findAllWithGallery()) {
            ExportData.BandDto dto = new ExportData.BandDto();
            dto.id = band.getId();
            dto.editionId = band.getEdition().getId();
            dto.name = band.getName();
            dto.genre = band.getGenre();
            dto.herkunft = band.getHerkunft();
            dto.description = band.getDescription();
            dto.performanceAt = band.getPerformanceAt();
            dto.mainImagePath = band.getMainImagePath();
            dto.youtubeVideoUrl = band.getYoutubeVideoUrl();
            dto.websiteUrl = band.getWebsiteUrl();
            dto.spotifyUrl = band.getSpotifyUrl();
            dto.instagramUrl = band.getInstagramUrl();
            dto.facebookUrl = band.getFacebookUrl();
            dto.youtubeUrl = band.getYoutubeUrl();
            dto.galleryImages.addAll(band.getGalleryImages());
            data.bands.add(dto);
        }

        for (FaqEntry faq : faqEntryRepository.findAll()) {
            ExportData.FaqEntryDto dto = new ExportData.FaqEntryDto();
            dto.id = faq.getId();
            dto.editionId = faq.getEdition().getId();
            dto.question = faq.getQuestion();
            dto.answer = faq.getAnswer();
            dto.sortOrder = faq.getSortOrder();
            data.faqEntries.add(dto);
        }

        for (NewsletterSubscriber subscriber : newsletterSubscriberRepository.findAll()) {
            ExportData.NewsletterSubscriberDto dto = new ExportData.NewsletterSubscriberDto();
            dto.id = subscriber.getId();
            dto.email = subscriber.getEmail();
            dto.subscribedAt = subscriber.getSubscribedAt();
            dto.unsubscribeToken = subscriber.getUnsubscribeToken();
            data.newsletterSubscribers.add(dto);
        }

        siteSettingsRepository.findById(SiteSettings.SINGLETON_ID).ifPresent(settings -> {
            ExportData.SiteSettingsDto dto = new ExportData.SiteSettingsDto();
            dto.logoImagePath = settings.getLogoImagePath();
            dto.faviconImagePath = settings.getFaviconImagePath();
            dto.instagramUrl = settings.getInstagramUrl();
            dto.facebookUrl = settings.getFacebookUrl();
            dto.contactEmail = settings.getContactEmail();
            dto.impressumText = settings.getImpressumText();
            dto.newsletterConfirmationSubject = settings.getNewsletterConfirmationSubject();
            dto.newsletterConfirmationBody = settings.getNewsletterConfirmationBody();
            data.siteSettings = dto;
        });

        return data;
    }

    /**
     * The show* getters on Edition apply "always shown on an archived edition"
     * fallback logic that only makes sense for rendering - export needs the raw
     * stored column values instead, straight from the DB, so re-reads them here
     * rather than adding raw-value getters to the entity just for this.
     */
    private void fillStoredToggles(ExportData data) {
        record Toggles(Boolean showEventInfos, Boolean showLineup, Boolean showFaq, Boolean showHeadliner) {
        }
        jdbcTemplate.query("SELECT id, show_event_infos, show_lineup, show_faq, show_headliner FROM editions", rs -> {
            long id = rs.getLong("id");
            Toggles toggles = new Toggles(
                    (Boolean) rs.getObject("show_event_infos"),
                    (Boolean) rs.getObject("show_lineup"),
                    (Boolean) rs.getObject("show_faq"),
                    (Boolean) rs.getObject("show_headliner"));
            data.editions.stream().filter(e -> e.id == id).findFirst().ifPresent(dto -> {
                dto.showEventInfos = toggles.showEventInfos();
                dto.showLineup = toggles.showLineup();
                dto.showFaq = toggles.showFaq();
                dto.showHeadliner = toggles.showHeadliner();
            });
        });
        jdbcTemplate.query("SELECT id, is_current FROM editions", rs -> {
            long id = rs.getLong("id");
            boolean current = rs.getBoolean("is_current");
            data.editions.stream().filter(e -> e.id == id).findFirst().ifPresent(dto -> dto.current = current);
        });
    }

    private void addUploadsToZip(ZipOutputStream zip) throws IOException {
        if (!Files.isDirectory(uploadRoot)) {
            return;
        }
        try (var walk = Files.walk(uploadRoot)) {
            for (Path path : (Iterable<Path>) walk.filter(Files::isRegularFile)::iterator) {
                String relative = uploadRoot.relativize(path).toString().replace('\\', '/');
                zip.putNextEntry(new ZipEntry("uploads/" + relative));
                Files.copy(path, zip);
                zip.closeEntry();
            }
        }
    }

    // ---------------------------------------------------------------- import

    public record ImportResult(int locations, int editions, int bands, int faqEntries, int newsletterSubscribers) {
        public String summary() {
            return locations + " Location(s), " + editions + " Edition(en), " + bands + " Band(s), "
                    + faqEntries + " FAQ-Einträge, " + newsletterSubscribers + " Newsletter-Abonnent(en).";
        }
    }

    public ImportResult importFrom(InputStream in) throws IOException {
        Path tempDir = Files.createTempDirectory("goa-import-");
        try {
            ExportData data = extract(in, tempDir);
            if (data == null) {
                throw new IllegalArgumentException("Die Datei enthält keine data.json - kein gültiger Export.");
            }
            replaceUploads(tempDir.resolve("uploads"));
            replaceDatabase(data);
            return new ImportResult(data.locations.size(), data.editions.size(), data.bands.size(),
                    data.faqEntries.size(), data.newsletterSubscribers.size());
        } finally {
            deleteRecursively(tempDir);
        }
    }

    private ExportData extract(InputStream in, Path tempDir) throws IOException {
        ExportData data = null;
        try (ZipInputStream zip = new ZipInputStream(in)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().equals("data.json")) {
                    data = objectMapper.readValue(zip.readAllBytes(), ExportData.class);
                } else if (!entry.isDirectory() && entry.getName().startsWith("uploads/")) {
                    Path target = tempDir.resolve(entry.getName()).normalize();
                    if (!target.startsWith(tempDir)) {
                        throw new IOException("Ungültiger Eintrag in der Zip-Datei: " + entry.getName());
                    }
                    Files.createDirectories(target.getParent());
                    Files.copy(zip, target, StandardCopyOption.REPLACE_EXISTING);
                }
                zip.closeEntry();
            }
        }
        return data;
    }

    /**
     * Replaces the whole uploads/ tree with the imported one - a no-op if the zip had
     * none. Clears uploadRoot's *contents* rather than deleting-and-recreating the
     * directory itself: in the docker-compose deployment it's a mounted volume, and
     * removing a mount point's own directory entry fails (device busy) even though
     * clearing what's inside it works fine.
     */
    private void replaceUploads(Path importedUploads) throws IOException {
        if (!Files.isDirectory(importedUploads)) {
            return;
        }
        clearDirectoryContents(uploadRoot);
        Files.createDirectories(uploadRoot);
        try (var walk = Files.walk(importedUploads)) {
            for (Path source : (Iterable<Path>) walk::iterator) {
                Path target = uploadRoot.resolve(importedUploads.relativize(source));
                if (Files.isDirectory(source)) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    /**
     * Wipes every table this feature covers and re-inserts the imported rows with
     * their original ids via plain JDBC - {@code @GeneratedValue(IDENTITY)} columns
     * only auto-generate an id when none is given, so an explicit id in the INSERT
     * is accepted as-is. The id sequences are then fast-forwarded past the highest
     * imported id so the next *new* row (created normally, through JPA) doesn't
     * collide with one just restored.
     *
     * <p>Runs through a {@link TransactionTemplate} rather than {@code @Transactional}
     * because this is called from another method on the same instance - a plain "this"
     * call bypasses Spring's transactional proxy entirely, so the annotation alone
     * would silently do nothing here.
     */
    private void replaceDatabase(ExportData data) {
        transactionTemplate.executeWithoutResult(status -> doReplaceDatabase(data));
    }

    private void doReplaceDatabase(ExportData data) {
        jdbcTemplate.update("DELETE FROM band_gallery_images");
        jdbcTemplate.update("DELETE FROM bands");
        jdbcTemplate.update("DELETE FROM faq_entries");
        jdbcTemplate.update("DELETE FROM editions");
        jdbcTemplate.update("DELETE FROM locations");
        jdbcTemplate.update("DELETE FROM newsletter_subscribers");
        jdbcTemplate.update("DELETE FROM site_settings");

        for (ExportData.LocationDto l : data.locations) {
            jdbcTemplate.update("INSERT INTO locations (id, name, street, zip_city) VALUES (?, ?, ?, ?)",
                    l.id, l.name, l.street, l.zipCity);
        }
        for (ExportData.EditionDto e : data.editions) {
            jdbcTemplate.update("INSERT INTO editions (id, type, festival_year, display_label, title, start_date, "
                            + "end_date, location_id, about_text, color_primary, color_secondary, color_accent, "
                            + "logo_image_path, background_image_path, location_image_path, show_event_infos, "
                            + "show_lineup, show_faq, show_headliner, is_current) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    e.id, e.type, e.year, e.displayLabel, e.title, e.startDate, e.endDate, e.locationId, e.aboutText,
                    e.colorPrimary, e.colorSecondary, e.colorAccent, e.logoImagePath, e.backgroundImagePath,
                    e.locationImagePath, e.showEventInfos, e.showLineup, e.showFaq, e.showHeadliner, e.current);
        }
        for (ExportData.BandDto b : data.bands) {
            jdbcTemplate.update("INSERT INTO bands (id, edition_id, name, genre, herkunft, description, "
                            + "performance_at, main_image_path, youtube_video_url, website_url, spotify_url, "
                            + "instagram_url, facebook_url, youtube_url) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    b.id, b.editionId, b.name, b.genre, b.herkunft, b.description, b.performanceAt, b.mainImagePath,
                    b.youtubeVideoUrl, b.websiteUrl, b.spotifyUrl, b.instagramUrl, b.facebookUrl, b.youtubeUrl);
            int position = 0;
            for (String imagePath : b.galleryImages) {
                jdbcTemplate.update("INSERT INTO band_gallery_images (band_id, position, image_path) VALUES (?, ?, ?)",
                        b.id, position++, imagePath);
            }
        }
        for (ExportData.FaqEntryDto f : data.faqEntries) {
            jdbcTemplate.update("INSERT INTO faq_entries (id, edition_id, question, answer, sort_order) "
                    + "VALUES (?, ?, ?, ?, ?)", f.id, f.editionId, f.question, f.answer, f.sortOrder);
        }
        for (ExportData.NewsletterSubscriberDto n : data.newsletterSubscribers) {
            jdbcTemplate.update("INSERT INTO newsletter_subscribers (id, email, subscribed_at, unsubscribe_token) "
                    + "VALUES (?, ?, ?, ?)", n.id, n.email, n.subscribedAt, n.unsubscribeToken);
        }
        if (data.siteSettings != null) {
            ExportData.SiteSettingsDto s = data.siteSettings;
            jdbcTemplate.update("INSERT INTO site_settings (id, logo_image_path, favicon_image_path, instagram_url, "
                            + "facebook_url, contact_email, impressum_text, newsletter_confirmation_subject, "
                            + "newsletter_confirmation_body) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    SiteSettings.SINGLETON_ID, s.logoImagePath, s.faviconImagePath, s.instagramUrl, s.facebookUrl,
                    s.contactEmail, s.impressumText, s.newsletterConfirmationSubject, s.newsletterConfirmationBody);
        }

        resetIdentitySequence("locations");
        resetIdentitySequence("editions");
        resetIdentitySequence("bands");
        resetIdentitySequence("faq_entries");
        resetIdentitySequence("newsletter_subscribers");
    }

    private void resetIdentitySequence(String table) {
        jdbcTemplate.queryForObject("SELECT setval(pg_get_serial_sequence('" + table + "', 'id'), "
                + "COALESCE((SELECT MAX(id) FROM " + table + "), 1), "
                + "(SELECT COUNT(*) FROM " + table + ") > 0)", Long.class);
    }

    private void deleteRecursively(Path dir) {
        if (Files.exists(dir)) {
            try {
                FileSystemUtils.deleteRecursively(dir);
            } catch (IOException e) {
                throw new UncheckedIOException("Konnte Verzeichnis nicht löschen: " + dir, e);
            }
        }
    }

    /** Deletes everything inside {@code dir}, but never {@code dir} itself. */
    private void clearDirectoryContents(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (var entries = Files.list(dir)) {
            for (Path entry : (Iterable<Path>) entries::iterator) {
                deleteRecursively(entry);
            }
        }
    }
}
