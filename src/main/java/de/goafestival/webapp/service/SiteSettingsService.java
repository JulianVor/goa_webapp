package de.goafestival.webapp.service;

import de.goafestival.webapp.domain.SiteSettings;
import de.goafestival.webapp.dto.SiteSettingsForm;
import de.goafestival.webapp.repository.SiteSettingsRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class SiteSettingsService {

    private final SiteSettingsRepository siteSettingsRepository;
    private final FileStorageService fileStorageService;

    public SiteSettingsService(SiteSettingsRepository siteSettingsRepository, FileStorageService fileStorageService) {
        this.siteSettingsRepository = siteSettingsRepository;
        this.fileStorageService = fileStorageService;
    }

    /** Returns the single global settings row, creating it on first access. */
    public SiteSettings get() {
        return siteSettingsRepository.findById(SiteSettings.SINGLETON_ID)
                .orElseGet(() -> siteSettingsRepository.save(new SiteSettings()));
    }

    public void update(SiteSettingsForm form) {
        SiteSettings settings = get();
        settings.setInstagramUrl(form.getInstagramUrl());
        settings.setFacebookUrl(form.getFacebookUrl());
        settings.setContactEmail(form.getContactEmail());
        settings.setImpressumText(form.getImpressumText());
        if (form.getLogoImage() != null && !form.getLogoImage().isEmpty()) {
            fileStorageService.delete(settings.getLogoImagePath());
            settings.setLogoImagePath(fileStorageService.store(form.getLogoImage(), "site",
                    FileStorageService.MAX_DIMENSION_STANDARD));
        }
        if (form.getFaviconImage() != null && !form.getFaviconImage().isEmpty()) {
            fileStorageService.delete(settings.getFaviconImagePath());
            settings.setFaviconImagePath(fileStorageService.store(form.getFaviconImage(), "site",
                    FileStorageService.MAX_DIMENSION_ICON));
        }
        siteSettingsRepository.save(settings);
    }

    public void updateNewsletterConfirmation(String subject, String body) {
        SiteSettings settings = get();
        settings.setNewsletterConfirmationSubject(subject);
        settings.setNewsletterConfirmationBody(body);
        siteSettingsRepository.save(settings);
    }

    /** Re-optimizes already-stored images (uploaded before automatic resizing existed). Returns how many files were rewritten. */
    public int optimizeImages() {
        SiteSettings settings = get();
        int count = 0;

        String newLogo = fileStorageService.reoptimize(settings.getLogoImagePath(), "site", FileStorageService.MAX_DIMENSION_STANDARD);
        if (newLogo != null) {
            fileStorageService.delete(settings.getLogoImagePath());
            settings.setLogoImagePath(newLogo);
            count++;
        }
        String newFavicon = fileStorageService.reoptimize(settings.getFaviconImagePath(), "site", FileStorageService.MAX_DIMENSION_ICON);
        if (newFavicon != null) {
            fileStorageService.delete(settings.getFaviconImagePath());
            settings.setFaviconImagePath(newFavicon);
            count++;
        }

        if (count > 0) {
            siteSettingsRepository.save(settings);
        }
        return count;
    }
}
