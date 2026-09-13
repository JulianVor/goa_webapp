package de.goafestival.webapp.web.admin;

import de.goafestival.webapp.service.BandService;
import de.goafestival.webapp.service.EditionService;
import de.goafestival.webapp.service.SiteSettingsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private final EditionService editionService;
    private final BandService bandService;
    private final SiteSettingsService siteSettingsService;

    public AdminDashboardController(EditionService editionService, BandService bandService,
                                     SiteSettingsService siteSettingsService) {
        this.editionService = editionService;
        this.bandService = bandService;
        this.siteSettingsService = siteSettingsService;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("editions", editionService.findAllOrdered());
        return "admin/dashboard";
    }

    /**
     * One-off cleanup for images uploaded before automatic resizing existed
     * (see FileStorageService): re-downscales/re-compresses every image still
     * referenced anywhere (site settings, editions, bands) in place.
     */
    @PostMapping("/optimize-images")
    public String optimizeImages(RedirectAttributes redirectAttributes) {
        int count = siteSettingsService.optimizeImages()
                + editionService.optimizeImages()
                + bandService.optimizeImages();
        redirectAttributes.addFlashAttribute("success", count == 0
                ? "Alle Bilder sind bereits optimiert."
                : count + " Bild(er) wurden optimiert.");
        return "redirect:/admin";
    }
}
