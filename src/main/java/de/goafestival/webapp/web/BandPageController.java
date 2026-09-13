package de.goafestival.webapp.web;

import de.goafestival.webapp.domain.Band;
import de.goafestival.webapp.service.BandService;
import de.goafestival.webapp.service.EditionService;
import de.goafestival.webapp.service.YoutubeUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class BandPageController {

    private final BandService bandService;
    private final EditionService editionService;

    public BandPageController(BandService bandService, EditionService editionService) {
        this.bandService = bandService;
        this.editionService = editionService;
    }

    @GetMapping("/bands/{id}")
    public String bandDetail(@PathVariable Long id, Model model) {
        Band band = bandService.getByIdWithEditionOrThrow(id);
        model.addAttribute("band", band);
        model.addAttribute("edition", band.getEdition());
        model.addAttribute("youtubeEmbedUrl", YoutubeUtils.toEmbedUrl(band.getYoutubeVideoUrl()));
        model.addAttribute("otherEditions", editionService.findOtherEditions(band.getEdition()));
        return "band-detail";
    }
}
