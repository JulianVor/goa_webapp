package de.goafestival.webapp.web.admin;

import de.goafestival.webapp.domain.Edition;
import de.goafestival.webapp.domain.FaqEntry;
import de.goafestival.webapp.dto.FaqEntryForm;
import de.goafestival.webapp.service.EditionService;
import de.goafestival.webapp.service.FaqEntryService;
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
@RequestMapping("/admin/editions/{editionId}/faq")
public class AdminFaqController {

    private final FaqEntryService faqEntryService;
    private final EditionService editionService;

    public AdminFaqController(FaqEntryService faqEntryService, EditionService editionService) {
        this.faqEntryService = faqEntryService;
        this.editionService = editionService;
    }

    @GetMapping
    public String list(@PathVariable Long editionId, Model model) {
        model.addAttribute("edition", editionService.getByIdOrThrow(editionId));
        model.addAttribute("faqEntries", faqEntryService.findByEdition(editionId));
        return "admin/faq-list";
    }

    @GetMapping("/new")
    public String newForm(@PathVariable Long editionId, Model model) {
        FaqEntryForm form = new FaqEntryForm();
        form.setEditionId(editionId);
        model.addAttribute("edition", editionService.getByIdOrThrow(editionId));
        model.addAttribute("faqEntryForm", form);
        model.addAttribute("isNew", true);
        return "admin/faq-form";
    }

    @GetMapping("/{faqId}/edit")
    public String editForm(@PathVariable Long editionId, @PathVariable Long faqId, Model model) {
        Edition edition = editionService.getByIdOrThrow(editionId);
        FaqEntry entry = faqEntryService.getByIdOrThrow(faqId);
        FaqEntryForm form = new FaqEntryForm();
        form.setId(entry.getId());
        form.setEditionId(editionId);
        form.setQuestion(entry.getQuestion());
        form.setAnswer(entry.getAnswer());
        form.setSortOrder(entry.getSortOrder());
        model.addAttribute("edition", edition);
        model.addAttribute("faqEntryForm", form);
        model.addAttribute("isNew", false);
        return "admin/faq-form";
    }

    @PostMapping
    public String create(@PathVariable Long editionId, @Valid @ModelAttribute("faqEntryForm") FaqEntryForm form,
                          BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        form.setEditionId(editionId);
        if (result.hasErrors()) {
            model.addAttribute("edition", editionService.getByIdOrThrow(editionId));
            model.addAttribute("isNew", true);
            return "admin/faq-form";
        }
        faqEntryService.create(form);
        redirectAttributes.addFlashAttribute("success", "FAQ-Eintrag wurde angelegt.");
        return "redirect:/admin/editions/" + editionId + "/faq";
    }

    @PostMapping("/{faqId}")
    public String update(@PathVariable Long editionId, @PathVariable Long faqId,
                          @Valid @ModelAttribute("faqEntryForm") FaqEntryForm form, BindingResult result,
                          Model model, RedirectAttributes redirectAttributes) {
        form.setEditionId(editionId);
        if (result.hasErrors()) {
            model.addAttribute("edition", editionService.getByIdOrThrow(editionId));
            model.addAttribute("isNew", false);
            return "admin/faq-form";
        }
        faqEntryService.update(faqId, form);
        redirectAttributes.addFlashAttribute("success", "FAQ-Eintrag wurde gespeichert.");
        return "redirect:/admin/editions/" + editionId + "/faq";
    }

    @PostMapping("/{faqId}/delete")
    public String delete(@PathVariable Long editionId, @PathVariable Long faqId, RedirectAttributes redirectAttributes) {
        faqEntryService.delete(faqId);
        redirectAttributes.addFlashAttribute("success", "FAQ-Eintrag wurde gelöscht.");
        return "redirect:/admin/editions/" + editionId + "/faq";
    }
}
