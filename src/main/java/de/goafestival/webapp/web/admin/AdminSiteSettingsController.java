package de.goafestival.webapp.web.admin;

import de.goafestival.webapp.dto.SiteSettingsForm;
import de.goafestival.webapp.service.SiteSettingsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/settings")
public class AdminSiteSettingsController {

    private final SiteSettingsService siteSettingsService;

    public AdminSiteSettingsController(SiteSettingsService siteSettingsService) {
        this.siteSettingsService = siteSettingsService;
    }

    @GetMapping
    public String form(Model model) {
        model.addAttribute("settingsForm", new SiteSettingsForm());
        return "admin/site-settings-form";
    }

    @PostMapping
    public String save(@ModelAttribute("settingsForm") SiteSettingsForm form, RedirectAttributes redirectAttributes) {
        siteSettingsService.update(form);
        redirectAttributes.addFlashAttribute("success", "Globale Einstellungen wurden gespeichert.");
        return "redirect:/admin/settings";
    }
}
