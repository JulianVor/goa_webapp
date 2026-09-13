package de.goafestival.webapp.service;

import de.goafestival.webapp.domain.Band;
import de.goafestival.webapp.domain.Edition;
import de.goafestival.webapp.dto.BandForm;
import de.goafestival.webapp.dto.DayLineup;
import de.goafestival.webapp.repository.BandRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class BandService {

    private final BandRepository bandRepository;
    private final EditionService editionService;
    private final FileStorageService fileStorageService;

    public BandService(BandRepository bandRepository, EditionService editionService, FileStorageService fileStorageService) {
        this.bandRepository = bandRepository;
        this.editionService = editionService;
        this.fileStorageService = fileStorageService;
    }

    public Band getByIdOrThrow(Long id) {
        return bandRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Band " + id + " wurde nicht gefunden."));
    }

    /** Like {@link #getByIdOrThrow(Long)} but also fetches the edition, for rendering outside a transaction. */
    public Band getByIdWithEditionOrThrow(Long id) {
        return bandRepository.findByIdWithEdition(id)
                .orElseThrow(() -> new NotFoundException("Band " + id + " wurde nicht gefunden."));
    }

    /** Like {@link #getByIdOrThrow(Long)} but also fetches the gallery images, for rendering outside a transaction. */
    public Band getByIdWithGalleryOrThrow(Long id) {
        return bandRepository.findByIdWithGallery(id)
                .orElseThrow(() -> new NotFoundException("Band " + id + " wurde nicht gefunden."));
    }

    public List<Band> findByEdition(Long editionId) {
        return bandRepository.findByEditionIdOrderByPerformanceAtAsc(editionId);
    }

    /** Groups bands by performance day, ordered chronologically by day and then by time. */
    public List<DayLineup> groupByDay(List<Band> bands) {
        Map<LocalDate, List<Band>> grouped = new LinkedHashMap<>();
        for (Band band : bands) {
            if (band.getPerformanceAt() == null) {
                continue;
            }
            grouped.computeIfAbsent(band.getPerformanceAt().toLocalDate(), d -> new ArrayList<>()).add(band);
        }
        List<DayLineup> result = new ArrayList<>();
        for (Map.Entry<LocalDate, List<Band>> entry : grouped.entrySet()) {
            result.add(new DayLineup(entry.getKey(), GermanDateFormats.dayLabel(entry.getKey()), entry.getValue()));
        }
        return result;
    }

    public Band create(BandForm form) {
        Band band = new Band();
        Edition edition = editionService.getByIdOrThrow(form.getEditionId());
        band.setEdition(edition);
        applyForm(band, form);
        return bandRepository.save(band);
    }

    public Band update(Long id, BandForm form) {
        Band band = getByIdOrThrow(id);
        applyForm(band, form);
        return bandRepository.save(band);
    }

    public void delete(Long id) {
        Band band = getByIdOrThrow(id);
        fileStorageService.delete(band.getMainImagePath());
        band.getGalleryImages().forEach(fileStorageService::delete);
        bandRepository.delete(band);
    }

    private void applyForm(Band band, BandForm form) {
        band.setName(form.getName());
        band.setGenre(form.getGenre());
        band.setHerkunft(form.getHerkunft());
        band.setDescription(form.getDescription());
        band.setPerformanceAt(form.getPerformanceAt());
        band.setYoutubeVideoUrl(form.getYoutubeVideoUrl());
        band.setWebsiteUrl(form.getWebsiteUrl());
        band.setSpotifyUrl(form.getSpotifyUrl());
        band.setInstagramUrl(form.getInstagramUrl());
        band.setFacebookUrl(form.getFacebookUrl());
        band.setYoutubeUrl(form.getYoutubeUrl());

        if (form.getMainImage() != null && !form.getMainImage().isEmpty()) {
            fileStorageService.delete(band.getMainImagePath());
            band.setMainImagePath(fileStorageService.store(form.getMainImage(), "bands/main"));
        }

        List<MultipartFile> uploaded = form.getGalleryImages() == null ? List.of() : form.getGalleryImages().stream()
                .filter(f -> f != null && !f.isEmpty())
                .collect(Collectors.toList());
        if (!uploaded.isEmpty()) {
            band.getGalleryImages().forEach(fileStorageService::delete);
            List<String> stored = uploaded.stream()
                    .limit(Band.MAX_GALLERY_IMAGES)
                    .map(f -> fileStorageService.store(f, "bands/gallery"))
                    .collect(Collectors.toList());
            band.setGalleryImages(stored);
        }
    }
}
