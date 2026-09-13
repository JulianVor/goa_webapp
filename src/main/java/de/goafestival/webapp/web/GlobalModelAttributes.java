package de.goafestival.webapp.web;

import de.goafestival.webapp.domain.SiteSettings;
import de.goafestival.webapp.service.SiteSettingsService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Makes the global site branding (nav logo, favicon) available as
 * {@code siteSettings} in every template, public and admin alike, without
 * every controller having to load and add it itself.
 */
@ControllerAdvice
public class GlobalModelAttributes {

    private final SiteSettingsService siteSettingsService;

    public GlobalModelAttributes(SiteSettingsService siteSettingsService) {
        this.siteSettingsService = siteSettingsService;
    }

    @ModelAttribute("siteSettings")
    public SiteSettings siteSettings() {
        return siteSettingsService.get();
    }
}
