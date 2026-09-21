package de.goafestival.webapp.web;

import de.goafestival.webapp.domain.Edition;
import de.goafestival.webapp.domain.SiteSettings;
import de.goafestival.webapp.service.EditionService;
import de.goafestival.webapp.service.NewsletterService;
import de.goafestival.webapp.service.RecaptchaService;
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
    private final NewsletterService newsletterService;
    private final RecaptchaService recaptchaService;

    public GlobalModelAttributes(SiteSettingsService siteSettingsService, EditionService editionService,
                                  NewsletterService newsletterService, RecaptchaService recaptchaService) {
        this.siteSettingsService = siteSettingsService;
        this.editionService = editionService;
        this.newsletterService = newsletterService;
        this.recaptchaService = recaptchaService;
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

    /** The current Festival edition, if any - lets a Kneipenkonzert fall back to its background image. */
    @ModelAttribute("currentEdition")
    public Edition currentEdition() {
        return editionService.findCurrent().orElse(null);
    }

    /** Whether the newsletter signup button/modal should render at all - only with a mail server configured. */
    @ModelAttribute("newsletterEnabled")
    public boolean newsletterEnabled() {
        return newsletterService.isEnabled();
    }

    /** The reCAPTCHA site key for the newsletter form, or "" if reCAPTCHA isn't configured (widget then just isn't shown). */
    @ModelAttribute("recaptchaSiteKey")
    public String recaptchaSiteKey() {
        return recaptchaService.getSiteKey();
    }
}
