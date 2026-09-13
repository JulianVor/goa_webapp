package de.goafestival.webapp.web;

import de.goafestival.webapp.domain.Edition;
import de.goafestival.webapp.service.EditionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ImpressumController {

    private final EditionService editionService;

    public ImpressumController(EditionService editionService) {
        this.editionService = editionService;
    }

    @GetMapping("/impressum")
    public String impressum(Model model) {
        Edition edition = editionService.getCurrentOrThrow();
        model.addAttribute("edition", edition);
        model.addAttribute("archivedEditions", editionService.findArchivedEditions());
        return "impressum";
    }
}
