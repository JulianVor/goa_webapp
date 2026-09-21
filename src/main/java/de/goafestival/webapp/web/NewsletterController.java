package de.goafestival.webapp.web;

import de.goafestival.webapp.domain.Edition;
import de.goafestival.webapp.dto.NewsletterSubscribeForm;
import de.goafestival.webapp.service.EditionService;
import de.goafestival.webapp.service.NewsletterService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;

/** The public newsletter signup (nav toolbar modal) and one-click unsubscribe link. */
@Controller
@RequestMapping("/newsletter")
public class NewsletterController {

    private final NewsletterService newsletterService;
    private final EditionService editionService;

    public NewsletterController(NewsletterService newsletterService, EditionService editionService) {
        this.newsletterService = newsletterService;
        this.editionService = editionService;
    }

    @PostMapping("/subscribe")
    public String subscribe(@Valid @ModelAttribute("newsletterSubscribeForm") NewsletterSubscribeForm form, BindingResult result,
                             @RequestHeader(value = "Referer", required = false) String referer,
                             RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("newsletterError", "Bitte eine gültige E-Mail-Adresse eingeben.");
        } else {
            try {
                newsletterService.subscribe(form.getEmail());
                redirectAttributes.addFlashAttribute("newsletterSuccess", "Danke! Du bist jetzt für den Newsletter angemeldet.");
            } catch (IllegalStateException e) {
                redirectAttributes.addFlashAttribute("newsletterError", e.getMessage());
            }
        }
        return "redirect:" + safeRedirectPath(referer);
    }

    @GetMapping("/unsubscribe")
    public String unsubscribe(@RequestParam String token, Model model) {
        newsletterService.unsubscribeByToken(token);
        Edition current = editionService.getCurrentOrThrow();
        model.addAttribute("edition", current);
        model.addAttribute("archivedEditions", editionService.findArchivedEditions());
        return "newsletter-unsubscribe";
    }

    /**
     * Only ever reuses the *path* (+ query) of the Referer header, never its scheme
     * or host, so a spoofed Referer can't turn this into an open redirect off-site.
     */
    private String safeRedirectPath(String referer) {
        if (referer == null) {
            return "/";
        }
        try {
            URI uri = URI.create(referer);
            String path = uri.getRawPath();
            if (path == null || path.isBlank() || path.startsWith("//")) {
                return "/";
            }
            String query = uri.getRawQuery();
            return query != null ? path + "?" + query : path;
        } catch (IllegalArgumentException e) {
            return "/";
        }
    }
}
