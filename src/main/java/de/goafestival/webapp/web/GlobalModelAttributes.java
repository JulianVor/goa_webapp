package de.goafestival.webapp.web;

import de.goafestival.webapp.domain.SiteSettings;
import de.goafestival.webapp.service.EditionService;
import de.goafestival.webapp.service.SiteSettingsService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Makes global, page-independent data available in every template, public
 * and admin alike, without every controller having to load and add it itself.
 */
@ControllerAdvice
public class GlobalModelAttributes {

    private final SiteSettingsService siteSettingsService;
    private final EditionService editionService;

    public GlobalModelAttributes(SiteSettingsService siteSettingsService, EditionService editionService) {
        this.siteSettingsService = siteSettingsService;
        this.editionService = editionService;
    }

    @ModelAttribute("siteSettings")
    public SiteSettings siteSettings() {
        return siteSettingsService.get();
    }

    /** Drives whether the nav's "Kneipenkonzerte" link is shown at all. */
    @ModelAttribute("hasKneipenkonzerte")
    public boolean hasKneipenkonzerte() {
        return editionService.hasKneipenkonzerte();
    }
}
