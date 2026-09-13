package de.goafestival.webapp.web.admin;

import de.goafestival.webapp.domain.Band;
import de.goafestival.webapp.domain.Edition;
import de.goafestival.webapp.dto.BandForm;
import de.goafestival.webapp.service.BandService;
import de.goafestival.webapp.service.EditionService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/editions/{editionId}/bands")
public class AdminBandController {

    private final BandService bandService;
    private final EditionService editionService;

    public AdminBandController(BandService bandService, EditionService editionService) {
        this.bandService = bandService;
        this.editionService = editionService;
    }

    @GetMapping
    public String list(@PathVariable Long editionId,
                        @RequestParam(required = false) Long sourceEditionId, Model model) {
        Edition edition = editionService.getByIdOrThrow(editionId);
        model.addAttribute("edition", edition);
        model.addAttribute("bands", bandService.findByEdition(editionId));
        model.addAttribute("otherEditions", editionService.findAllExcept(editionId));
        if (sourceEditionId != null) {
            model.addAttribute("sourceEdition", editionService.getByIdOrThrow(sourceEditionId));
            model.addAttribute("sourceBands", bandService.findByEdition(sourceEditionId));
        }
        return "admin/bands-list";
    }

    @GetMapping("/new")
    public String newForm(@PathVariable Long editionId, Model model) {
        Edition edition = editionService.getByIdOrThrow(editionId);
        BandForm form = new BandForm();
        form.setEditionId(editionId);
        model.addAttribute("edition", edition);
        model.addAttribute("bandForm", form);
        model.addAttribute("isNew", true);
        return "admin/band-form";
    }

    @GetMapping("/{bandId}/edit")
    public String editForm(@PathVariable Long editionId, @PathVariable Long bandId, Model model) {
        Edition edition = editionService.getByIdOrThrow(editionId);
        Band band = bandService.getByIdWithGalleryOrThrow(bandId);
        model.addAttribute("edition", edition);
        model.addAttribute("band", band);
        model.addAttribute("bandForm", toForm(band));
        model.addAttribute("isNew", false);
        return "admin/band-form";
    }

    @PostMapping
    public String create(@PathVariable Long editionId, @Valid @ModelAttribute("bandForm") BandForm form, BindingResult result,
                          Model model, RedirectAttributes redirectAttributes) {
        form.setEditionId(editionId);
        if (result.hasErrors()) {
            model.addAttribute("edition", editionService.getByIdOrThrow(editionId));
            model.addAttribute("isNew", true);
            return "admin/band-form";
        }
        Band saved = bandService.create(form);
        redirectAttributes.addFlashAttribute("success", "Band \"" + saved.getName() + "\" wurde angelegt.");
        return "redirect:/admin/editions/" + editionId + "/bands";
    }

    @PostMapping("/{bandId}")
    public String update(@PathVariable Long editionId, @PathVariable Long bandId,
                          @Valid @ModelAttribute("bandForm") BandForm form, BindingResult result,
                          Model model, RedirectAttributes redirectAttributes) {
        form.setEditionId(editionId);
        if (result.hasErrors()) {
            model.addAttribute("edition", editionService.getByIdOrThrow(editionId));
            model.addAttribute("band", bandService.getByIdWithGalleryOrThrow(bandId));
            model.addAttribute("isNew", false);
            return "admin/band-form";
        }
        Band saved = bandService.update(bandId, form);
        redirectAttributes.addFlashAttribute("success", "Band \"" + saved.getName() + "\" wurde gespeichert.");
        return "redirect:/admin/editions/" + editionId + "/bands";
    }

    @PostMapping("/{bandId}/delete")
    public String delete(@PathVariable Long editionId, @PathVariable Long bandId, RedirectAttributes redirectAttributes) {
        bandService.delete(bandId);
        redirectAttributes.addFlashAttribute("success", "Band wurde gelöscht.");
        return "redirect:/admin/editions/" + editionId + "/bands";
    }

    @PostMapping("/copy")
    public String copy(@PathVariable Long editionId, @RequestParam Long sourceBandId, @RequestParam Long sourceEditionId,
                        RedirectAttributes redirectAttributes) {
        Band copy = bandService.copyToEdition(sourceBandId, editionId);
        redirectAttributes.addFlashAttribute("success",
                "Band \"" + copy.getName() + "\" wurde übernommen. Bitte Auftrittszeit prüfen und ergänzen.");
        return "redirect:/admin/editions/" + editionId + "/bands?sourceEditionId=" + sourceEditionId;
    }

    private BandForm toForm(Band band) {
        BandForm form = new BandForm();
        form.setId(band.getId());
        form.setEditionId(band.getEdition().getId());
        form.setName(band.getName());
        form.setGenre(band.getGenre());
        form.setHerkunft(band.getHerkunft());
        form.setDescription(band.getDescription());
        form.setPerformanceAt(band.getPerformanceAt());
        form.setYoutubeVideoUrl(band.getYoutubeVideoUrl());
        form.setWebsiteUrl(band.getWebsiteUrl());
        form.setSpotifyUrl(band.getSpotifyUrl());
        form.setInstagramUrl(band.getInstagramUrl());
        form.setFacebookUrl(band.getFacebookUrl());
        form.setYoutubeUrl(band.getYoutubeUrl());
        return form;
    }
}
