package de.goafestival.webapp.web.admin;

import de.goafestival.webapp.domain.SiteSettings;
import de.goafestival.webapp.dto.NewsletterConfirmationForm;
import de.goafestival.webapp.dto.SiteSettingsForm;
import de.goafestival.webapp.service.NewsletterService;
import de.goafestival.webapp.service.SiteSettingsService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/settings")
public class AdminSiteSettingsController {

    private final SiteSettingsService siteSettingsService;
    private final NewsletterService newsletterService;

    public AdminSiteSettingsController(SiteSettingsService siteSettingsService, NewsletterService newsletterService) {
        this.siteSettingsService = siteSettingsService;
        this.newsletterService = newsletterService;
    }

    @GetMapping
    public String form(Model model) {
        model.addAttribute("settingsForm", toForm(siteSettingsService.get()));
        return "admin/site-settings-form";
    }

    @PostMapping
    public String save(@ModelAttribute("settingsForm") SiteSettingsForm form, RedirectAttributes redirectAttributes) {
        siteSettingsService.update(form);
        redirectAttributes.addFlashAttribute("success", "Globale Einstellungen wurden gespeichert.");
        return "redirect:/admin/settings";
    }

    @GetMapping("/newsletter-confirmation")
    public String newsletterConfirmationForm(Model model) {
        model.addAttribute("confirmationForm", confirmationFormFromCurrent());
        return "admin/newsletter-confirmation";
    }

    @PostMapping("/newsletter-confirmation")
    public String saveNewsletterConfirmation(@Valid @ModelAttribute("confirmationForm") NewsletterConfirmationForm form,
                                              BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "admin/newsletter-confirmation";
        }
        newsletterService.updateConfirmationTemplate(form.getSubject(), form.getHtmlBody());
        redirectAttributes.addFlashAttribute("success", "Bestätigungsmail wurde gespeichert.");
        return "redirect:/admin/settings/newsletter-confirmation";
    }

    private NewsletterConfirmationForm confirmationFormFromCurrent() {
        NewsletterConfirmationForm form = new NewsletterConfirmationForm();
        form.setSubject(newsletterService.getConfirmationSubject());
        form.setHtmlBody(newsletterService.getConfirmationBody());
        return form;
    }

    private SiteSettingsForm toForm(SiteSettings settings) {
        SiteSettingsForm form = new SiteSettingsForm();
        form.setInstagramUrl(settings.getInstagramUrl());
        form.setFacebookUrl(settings.getFacebookUrl());
        form.setContactEmail(settings.getContactEmail());
        form.setImpressumText(settings.getImpressumText());
        return form;
    }
}
