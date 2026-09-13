package de.goafestival.webapp.web.admin;

import de.goafestival.webapp.domain.Edition;
import de.goafestival.webapp.dto.EditionForm;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/editions")
public class AdminEditionController {

    private final EditionService editionService;

    public AdminEditionController(EditionService editionService) {
        this.editionService = editionService;
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("editionForm", new EditionForm());
        model.addAttribute("isNew", true);
        return "admin/edition-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Edition edition = editionService.getByIdOrThrow(id);
        model.addAttribute("editionForm", toForm(edition));
        model.addAttribute("edition", edition);
        model.addAttribute("isNew", false);
        return "admin/edition-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("editionForm") EditionForm form, BindingResult result, Model model,
                          RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("isNew", true);
            return "admin/edition-form";
        }
        Edition saved = editionService.create(form);
        redirectAttributes.addFlashAttribute("success", "Ausgabe \"" + saved.getTitle() + " " + saved.getYear() + "\" wurde angelegt.");
        return "redirect:/admin";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("editionForm") EditionForm form, BindingResult result,
                          Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("edition", editionService.getByIdOrThrow(id));
            model.addAttribute("isNew", false);
            return "admin/edition-form";
        }
        Edition saved = editionService.update(id, form);
        redirectAttributes.addFlashAttribute("success", "Ausgabe \"" + saved.getTitle() + " " + saved.getYear() + "\" wurde gespeichert.");
        return "redirect:/admin";
    }

    @PostMapping("/{id}/set-current")
    public String setCurrent(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        editionService.setCurrent(id);
        redirectAttributes.addFlashAttribute("success", "Ausgabe wurde als aktuelle Festival-Seite gesetzt.");
        return "redirect:/admin";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        editionService.delete(id);
        redirectAttributes.addFlashAttribute("success", "Ausgabe wurde gelöscht.");
        return "redirect:/admin";
    }

    private EditionForm toForm(Edition edition) {
        EditionForm form = new EditionForm();
        form.setId(edition.getId());
        form.setYear(edition.getYear());
        form.setDisplayLabel(edition.getDisplayLabel());
        form.setTitle(edition.getTitle());
        form.setHeadlinerName(edition.getHeadlinerName());
        form.setHeadlinerDateLabel(edition.getHeadlinerDateLabel());
        form.setStartDate(edition.getStartDate());
        form.setEndDate(edition.getEndDate());
        form.setLocationName(edition.getLocationName());
        form.setLocationStreet(edition.getLocationStreet());
        form.setLocationZipCity(edition.getLocationZipCity());
        form.setAboutText(edition.getAboutText());
        form.setColorPrimary(edition.getColorPrimary());
        form.setColorSecondary(edition.getColorSecondary());
        form.setColorAccent(edition.getColorAccent());
        form.setColorText(edition.getColorText());
        form.setCurrent(edition.isCurrent());
        form.setShowEventInfos(edition.isShowEventInfos());
        form.setShowLineup(edition.isShowLineup());
        form.setShowFaq(edition.isShowFaq());
        form.setShowHeadliner(edition.isShowHeadliner());
        return form;
    }
}
