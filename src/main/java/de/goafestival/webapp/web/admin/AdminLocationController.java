package de.goafestival.webapp.web.admin;

import de.goafestival.webapp.domain.Location;
import de.goafestival.webapp.dto.LocationForm;
import de.goafestival.webapp.service.LocationService;
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
@RequestMapping("/admin/locations")
public class AdminLocationController {

    private final LocationService locationService;

    public AdminLocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("locations", locationService.findAllOrdered());
        return "admin/locations-list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("locationForm", new LocationForm());
        model.addAttribute("isNew", true);
        return "admin/location-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Location location = locationService.getByIdOrThrow(id);
        LocationForm form = new LocationForm();
        form.setId(location.getId());
        form.setName(location.getName());
        form.setStreet(location.getStreet());
        form.setZipCity(location.getZipCity());
        model.addAttribute("locationForm", form);
        model.addAttribute("isNew", false);
        return "admin/location-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("locationForm") LocationForm form, BindingResult result, Model model,
                          RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("isNew", true);
            return "admin/location-form";
        }
        Location saved = locationService.create(form);
        redirectAttributes.addFlashAttribute("success", "Location \"" + saved.getName() + "\" wurde angelegt.");
        return "redirect:/admin/locations";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("locationForm") LocationForm form, BindingResult result,
                          Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("isNew", false);
            return "admin/location-form";
        }
        Location saved = locationService.update(id, form);
        redirectAttributes.addFlashAttribute("success", "Location \"" + saved.getName() + "\" wurde gespeichert.");
        return "redirect:/admin/locations";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            locationService.delete(id);
            redirectAttributes.addFlashAttribute("success", "Location wurde gelöscht.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/locations";
    }
}
