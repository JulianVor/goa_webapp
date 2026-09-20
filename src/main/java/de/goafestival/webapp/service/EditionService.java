package de.goafestival.webapp.service;

import de.goafestival.webapp.domain.Edition;
import de.goafestival.webapp.domain.EditionType;
import de.goafestival.webapp.dto.EditionForm;
import de.goafestival.webapp.repository.EditionRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class EditionService {

    private final EditionRepository editionRepository;
    private final FileStorageService fileStorageService;

    public EditionService(EditionRepository editionRepository, FileStorageService fileStorageService) {
        this.editionRepository = editionRepository;
        this.fileStorageService = fileStorageService;
    }

    public List<Edition> findAllOrdered() {
        return editionRepository.findAllByOrderByYearDesc();
    }

    public Edition getCurrentOrThrow() {
        return editionRepository.findByCurrentTrue()
                .orElseThrow(() -> new NotFoundException("Keine aktuelle Festival-Ausgabe konfiguriert."));
    }

    /** Looks up a FESTIVAL-type edition by year — the "/goa/{year}" route. */
    public Edition getByYearOrThrow(int year) {
        return editionRepository.findByYearAndType(year, EditionType.FESTIVAL)
                .orElseThrow(() -> new NotFoundException("Ausgabe " + year + " wurde nicht gefunden."));
    }

    public Edition getByIdOrThrow(Long id) {
        return editionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ausgabe " + id + " wurde nicht gefunden."));
    }

    public Edition getKneipenkonzertByIdOrThrow(Long id) {
        Edition edition = getByIdOrThrow(id);
        if (edition.getType() != EditionType.KNEIPENKONZERT) {
            throw new NotFoundException("Kneipenkonzert " + id + " wurde nicht gefunden.");
        }
        return edition;
    }

    /**
     * All non-current FESTIVAL editions, newest year first — the nav's "Archiv"
     * link/dropdown. Same result on every page, regardless of which edition is
     * being viewed; the current edition never appears here. Kneipenkonzerte have
     * their own list at "/kneipenkonzerte" and never show up in this dropdown.
     */
    public List<Edition> findArchivedEditions() {
        return findAllOrdered().stream()
                .filter(e -> e.getType() == EditionType.FESTIVAL && !e.isCurrent())
                .toList();
    }

    /** All Kneipenkonzerte, most recent start date first. */
    public List<Edition> findKneipenkonzerte() {
        return editionRepository.findByTypeOrderByStartDateDesc(EditionType.KNEIPENKONZERT);
    }

    /** Whether at least one Kneipenkonzert exists — drives the nav link's visibility. */
    public boolean hasKneipenkonzerte() {
        return editionRepository.existsByType(EditionType.KNEIPENKONZERT);
    }

    /** The soonest upcoming Kneipenkonzert, if any — the homepage teaser under the logo. */
    public Optional<Edition> findNextKneipenkonzert() {
        LocalDate today = LocalDate.now();
        return findKneipenkonzerte().stream()
                .filter(e -> e.getStartDate() != null && !e.getStartDate().isBefore(today))
                .min(Comparator.comparing(Edition::getStartDate));
    }

    /** All editions except the given one, newest year first — for "copy from another year" pickers. */
    public List<Edition> findAllExcept(Long editionId) {
        return findAllOrdered().stream()
                .filter(e -> !e.getId().equals(editionId))
                .toList();
    }

    public Edition create(EditionForm form) {
        Edition edition = new Edition();
        applyForm(edition, form);
        Edition saved = editionRepository.save(edition);
        if (form.isCurrent() && form.getType() == EditionType.FESTIVAL) {
            setCurrent(saved.getId());
        }
        return saved;
    }

    public Edition update(Long id, EditionForm form) {
        Edition edition = getByIdOrThrow(id);
        applyForm(edition, form);
        Edition saved = editionRepository.save(edition);
        if (form.isCurrent() && form.getType() == EditionType.FESTIVAL) {
            setCurrent(saved.getId());
        }
        return saved;
    }

    /** Marks the given edition as current and demotes every other edition. */
    public void setCurrent(Long id) {
        Edition target = getByIdOrThrow(id);
        editionRepository.findByCurrentTrue()
                .filter(e -> !e.getId().equals(id))
                .ifPresent(previous -> {
                    previous.setCurrent(false);
                    editionRepository.save(previous);
                });
        target.setCurrent(true);
        editionRepository.save(target);
    }

    public void delete(Long id) {
        Edition edition = getByIdOrThrow(id);
        fileStorageService.delete(edition.getLogoImagePath());
        fileStorageService.delete(edition.getBackgroundImagePath());
        fileStorageService.delete(edition.getLocationImagePath());
        editionRepository.delete(edition);
    }

    /** Re-optimizes already-stored images (uploaded before automatic resizing existed). Returns how many files were rewritten. */
    public int optimizeImages() {
        int count = 0;
        for (Edition edition : findAllOrdered()) {
            boolean changed = false;

            String newLogo = fileStorageService.reoptimize(edition.getLogoImagePath(), "editions/logos", FileStorageService.MAX_DIMENSION_STANDARD);
            if (newLogo != null) {
                fileStorageService.delete(edition.getLogoImagePath());
                edition.setLogoImagePath(newLogo);
                changed = true;
                count++;
            }
            String newBackground = fileStorageService.reoptimize(edition.getBackgroundImagePath(), "editions/backgrounds", FileStorageService.MAX_DIMENSION_BACKGROUND);
            if (newBackground != null) {
                fileStorageService.delete(edition.getBackgroundImagePath());
                edition.setBackgroundImagePath(newBackground);
                changed = true;
                count++;
            }
            String newLocation = fileStorageService.reoptimize(edition.getLocationImagePath(), "editions/location", FileStorageService.MAX_DIMENSION_STANDARD);
            if (newLocation != null) {
                fileStorageService.delete(edition.getLocationImagePath());
                edition.setLocationImagePath(newLocation);
                changed = true;
                count++;
            }

            if (changed) {
                editionRepository.save(edition);
            }
        }
        return count;
    }

    private void applyForm(Edition edition, EditionForm form) {
        edition.setType(form.getType());
        if (form.getType() == EditionType.KNEIPENKONZERT) {
            edition.setCurrent(false);
        }
        edition.setYear(form.getYear());
        edition.setDisplayLabel(form.getDisplayLabel());
        edition.setTitle(form.getTitle());
        edition.setHeadlinerName(form.getHeadlinerName());
        edition.setHeadlinerDateLabel(form.getHeadlinerDateLabel());
        edition.setStartDate(form.getStartDate());
        edition.setEndDate(form.getEndDate());
        edition.setLocationName(form.getLocationName());
        edition.setLocationStreet(form.getLocationStreet());
        edition.setLocationZipCity(form.getLocationZipCity());
        edition.setAboutText(form.getAboutText());
        edition.setColorPrimary(form.getColorPrimary());
        edition.setColorSecondary(form.getColorSecondary());
        edition.setColorAccent(form.getColorAccent());
        edition.setColorText(form.getColorText());
        edition.setShowEventInfos(form.isShowEventInfos());
        edition.setShowLineup(form.isShowLineup());
        edition.setShowFaq(form.isShowFaq());
        edition.setShowHeadliner(form.isShowHeadliner());

        if (form.getLogoImage() != null && !form.getLogoImage().isEmpty()) {
            fileStorageService.delete(edition.getLogoImagePath());
            edition.setLogoImagePath(fileStorageService.store(form.getLogoImage(), "editions/logos",
                    FileStorageService.MAX_DIMENSION_STANDARD));
        }
        if (form.getBackgroundImage() != null && !form.getBackgroundImage().isEmpty()) {
            fileStorageService.delete(edition.getBackgroundImagePath());
            edition.setBackgroundImagePath(fileStorageService.store(form.getBackgroundImage(), "editions/backgrounds",
                    FileStorageService.MAX_DIMENSION_BACKGROUND));
        }
        if (form.getLocationImage() != null && !form.getLocationImage().isEmpty()) {
            fileStorageService.delete(edition.getLocationImagePath());
            edition.setLocationImagePath(fileStorageService.store(form.getLocationImage(), "editions/location",
                    FileStorageService.MAX_DIMENSION_STANDARD));
        }
    }
}
