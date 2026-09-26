package de.goafestival.webapp.web.admin;

import de.goafestival.webapp.dto.NewsletterSendForm;
import de.goafestival.webapp.service.NewsletterService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/newsletter")
public class AdminNewsletterController {

    private final NewsletterService newsletterService;

    public AdminNewsletterController(NewsletterService newsletterService) {
        this.newsletterService = newsletterService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("subscribers", newsletterService.findAllOrdered());
        model.addAttribute("sendForm", new NewsletterSendForm());
        return "admin/newsletter-list";
    }

    /** Renders the exact branded HTML the admin's current draft would produce, for the preview-before-send step. */
    @PostMapping(value = "/preview", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String preview(@RequestParam(defaultValue = "") String htmlBody) {
        return newsletterService.previewHtml(htmlBody);
    }

    @PostMapping("/send")
    public String send(@Valid @ModelAttribute("sendForm") NewsletterSendForm form, BindingResult result,
                        Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("subscribers", newsletterService.findAllOrdered());
            return "admin/newsletter-list";
        }
        try {
            int sent = newsletterService.sendNewsletter(form.getSubject(), form.getHtmlBody());
            redirectAttributes.addFlashAttribute("success",
                    "Newsletter wurde an " + sent + " Abonnent" + (sent == 1 ? "" : "en") + " versendet.");
        } catch (NewsletterService.NewsletterSendException e) {
            redirectAttributes.addFlashAttribute("error",
                    e.getMessage() + " " + e.getSentBeforeFailure() + " E-Mail(s) wurden davor bereits erfolgreich versendet.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/newsletter";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        newsletterService.delete(id);
        redirectAttributes.addFlashAttribute("success", "Abonnent wurde gelöscht.");
        return "redirect:/admin/newsletter";
    }
}
