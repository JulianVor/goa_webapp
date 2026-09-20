package de.goafestival.webapp.web;

import de.goafestival.webapp.domain.Edition;
import de.goafestival.webapp.domain.SiteSettings;
import de.goafestival.webapp.service.EditionService;
import de.goafestival.webapp.service.SiteSettingsService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

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

    /**
     * Drives the nav's Kneipenkonzert link: hidden when empty, links straight to
     * the single Kneipenkonzert when there's exactly one, otherwise links to the
     * "/kneipenkonzerte" overview.
     */
    @ModelAttribute("navKneipenkonzerte")
    public List<Edition> navKneipenkonzerte() {
        return editionService.findKneipenkonzerte();
    }
}
