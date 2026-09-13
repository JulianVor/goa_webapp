package de.goafestival.webapp.web.admin;

import de.goafestival.webapp.service.EditionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private final EditionService editionService;

    public AdminDashboardController(EditionService editionService) {
        this.editionService = editionService;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("editions", editionService.findAllOrdered());
        return "admin/dashboard";
    }
}
