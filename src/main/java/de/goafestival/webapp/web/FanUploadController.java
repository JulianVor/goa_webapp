package de.goafestival.webapp.web;

import de.goafestival.webapp.domain.Edition;
import de.goafestival.webapp.service.EditionService;
import de.goafestival.webapp.service.FanUploadService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * The public "upload your festival photos/videos" page - deliberately not linked from
 * the site's own navigation anywhere, only reachable via a direct link handed out after
 * the fact (e.g. in an Instagram story). Follows the same URL shape as the edition pages
 * themselves: "/upload" for whichever edition is current, "/goa/{year}/upload" for any
 * (including archived) Festival year. No login - anyone with the link can submit; what's
 * uploaded is only visible again via the admin portal (see AdminFanUploadController).
 */
@Controller
public class FanUploadController {

    private final EditionService editionService;
    private final FanUploadService fanUploadService;

    public FanUploadController(EditionService editionService, FanUploadService fanUploadService) {
        this.editionService = editionService;
        this.fanUploadService = fanUploadService;
    }

    @GetMapping("/upload")
    public String currentUploadForm(Model model) {
        return uploadForm(editionService.getCurrentOrThrow(), "/upload", model);
    }

    @PostMapping("/upload")
    public String currentUpload(@RequestParam(value = "files", required = false) List<MultipartFile> files,
                                 RedirectAttributes redirectAttributes) {
        return handleUpload(editionService.getCurrentOrThrow(), files, "/upload", redirectAttributes);
    }

    @GetMapping("/goa/{year}/upload")
    public String yearUploadForm(@PathVariable int year, Model model) {
        return uploadForm(editionService.getByYearOrThrow(year), "/goa/" + year + "/upload", model);
    }

    @PostMapping("/goa/{year}/upload")
    public String yearUpload(@PathVariable int year,
                              @RequestParam(value = "files", required = false) List<MultipartFile> files,
                              RedirectAttributes redirectAttributes) {
        return handleUpload(editionService.getByYearOrThrow(year), files, "/goa/" + year + "/upload", redirectAttributes);
    }

    private String uploadForm(Edition edition, String uploadAction, Model model) {
        model.addAttribute("edition", edition);
        model.addAttribute("archivedEditions", editionService.findArchivedEditions());
        model.addAttribute("uploadAction", uploadAction);
        model.addAttribute("pageTitle", "Fotos & Videos hochladen – " + edition.getTitle());
        return "fan-upload";
    }

    private String handleUpload(Edition edition, List<MultipartFile> files, String uploadAction,
                                 RedirectAttributes redirectAttributes) {
        int stored = fanUploadService.storeAll(edition, files);
        if (stored > 0) {
            redirectAttributes.addFlashAttribute("uploadSuccess", stored == 1
                    ? "Danke! Deine Datei wurde hochgeladen."
                    : "Danke! Deine " + stored + " Dateien wurden hochgeladen.");
        } else {
            redirectAttributes.addFlashAttribute("uploadError", "Es wurde nichts hochgeladen - bitte wähle Fotos oder Videos aus.");
        }
        return "redirect:" + uploadAction;
    }
}
