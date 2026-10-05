package de.goafestival.webapp.web;

import de.goafestival.webapp.domain.Band;
import de.goafestival.webapp.service.BandService;
import de.goafestival.webapp.service.EditionService;
import de.goafestival.webapp.service.YoutubeUtils;
import de.goafestival.webapp.service.sharecard.BandShareCardService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;

@Controller
public class BandPageController {

    private final BandService bandService;
    private final EditionService editionService;
    private final BandShareCardService bandShareCardService;

    public BandPageController(BandService bandService, EditionService editionService,
                               BandShareCardService bandShareCardService) {
        this.bandService = bandService;
        this.editionService = editionService;
        this.bandShareCardService = bandShareCardService;
    }

    @GetMapping("/bands/{id}")
    public String bandDetail(@PathVariable Long id, Model model) {
        Band band = bandService.getByIdWithEditionOrThrow(id);
        model.addAttribute("band", band);
        model.addAttribute("edition", band.getEdition());
        model.addAttribute("youtubeEmbedUrl", YoutubeUtils.toEmbedUrl(band.getYoutubeVideoUrl()));
        model.addAttribute("archivedEditions", editionService.findArchivedEditions());
        return "band-detail";
    }

    /** A shareable "trading card" PNG for this band - see BandShareCardService. */
    @GetMapping("/bands/{id}/share-card.png")
    @ResponseBody
    public ResponseEntity<byte[]> shareCard(@PathVariable Long id) throws IOException {
        Band band = bandService.getByIdWithEditionOrThrow(id);
        byte[] png = bandShareCardService.render(band);
        String filename = "goa-" + band.getName().toLowerCase().replaceAll("[^a-z0-9]+", "-") + ".png";
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(png);
    }
}
