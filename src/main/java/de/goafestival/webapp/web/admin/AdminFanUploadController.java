package de.goafestival.webapp.web.admin;

import de.goafestival.webapp.domain.FanUpload;
import de.goafestival.webapp.service.EditionService;
import de.goafestival.webapp.service.FanUploadService;
import de.goafestival.webapp.service.FanUploadStorageService;
import de.goafestival.webapp.service.NotFoundException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Where fan-uploaded photos/videos actually become visible - protected by the same
 * "/admin/**" login as the rest of the admin area (see SecurityConfig). The file itself
 * is streamed from FanUploadStorageService's private directory, never through the
 * public "/uploads/**" mapping other uploads use.
 */
@Controller
@RequestMapping("/admin/fan-uploads")
public class AdminFanUploadController {

    private final FanUploadService fanUploadService;
    private final FanUploadStorageService storageService;
    private final EditionService editionService;

    public AdminFanUploadController(FanUploadService fanUploadService, FanUploadStorageService storageService,
                                     EditionService editionService) {
        this.fanUploadService = fanUploadService;
        this.storageService = storageService;
        this.editionService = editionService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) Long editionId, Model model) {
        List<FanUpload> uploads = editionId != null
                ? fanUploadService.findByEdition(editionId)
                : fanUploadService.findAll();
        model.addAttribute("uploads", uploads);
        model.addAttribute("editions", editionService.findFestivalEditions());
        model.addAttribute("selectedEditionId", editionId);
        return "admin/fan-uploads-list";
    }

    /** Streams the original file - inline, so an image/video previews directly; not cached, since it's not public. */
    @GetMapping("/{id}/file")
    @ResponseBody
    public ResponseEntity<Resource> file(@PathVariable Long id) throws IOException {
        FanUpload upload = fanUploadService.getByIdOrThrow(id);
        Path path = storageService.resolve(upload.getStoredPath());
        if (!Files.exists(path)) {
            throw new NotFoundException("Datei wurde nicht gefunden.");
        }
        Resource resource = new FileSystemResource(path);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(upload.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + upload.getOriginalFilename() + "\"")
                .contentLength(Files.size(path))
                .body(resource);
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        fanUploadService.delete(id);
        redirectAttributes.addFlashAttribute("success", "Datei wurde gelöscht.");
        return "redirect:/admin/fan-uploads";
    }
}
