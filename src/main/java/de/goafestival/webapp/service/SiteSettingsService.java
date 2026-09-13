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
        if (form.getLogoImage() != null && !form.getLogoImage().isEmpty()) {
            fileStorageService.delete(settings.getLogoImagePath());
            settings.setLogoImagePath(fileStorageService.store(form.getLogoImage(), "site"));
        }
        if (form.getFaviconImage() != null && !form.getFaviconImage().isEmpty()) {
            fileStorageService.delete(settings.getFaviconImagePath());
            settings.setFaviconImagePath(fileStorageService.store(form.getFaviconImage(), "site"));
        }
        siteSettingsRepository.save(settings);
    }
}
