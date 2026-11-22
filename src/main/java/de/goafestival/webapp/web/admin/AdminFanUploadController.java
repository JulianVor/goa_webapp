package de.goafestival.webapp.web.admin;

import de.goafestival.webapp.domain.FanUpload;
import de.goafestival.webapp.service.EditionService;
import de.goafestival.webapp.service.FanUploadService;
import de.goafestival.webapp.service.FanUploadStorageService;
import de.goafestival.webapp.service.NotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
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
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

    /**
     * Streams every upload matching the current filter as one ZIP - built on the fly,
     * nothing written to disk first. Photos/videos are already-compressed formats, so the
     * zip uses no further compression (Deflater.NO_COMPRESSION) rather than burning CPU
     * for no size benefit. Entry names are prefixed with the upload's id, since original
     * filenames (phone camera names like "IMG_0001.jpg") collide across different uploads.
     */
    @GetMapping("/download-all")
    public void downloadAll(@RequestParam(required = false) Long editionId, HttpServletResponse response) throws IOException {
        List<FanUpload> uploads = editionId != null
                ? fanUploadService.findByEdition(editionId)
                : fanUploadService.findAll();
        String label = editionId != null ? String.valueOf(editionService.getByIdOrThrow(editionId).getYear()) : "alle-jahre";

        response.setContentType("application/zip");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"goa-fan-uploads-" + label + ".zip\"");

        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            zos.setLevel(Deflater.NO_COMPRESSION);
            for (FanUpload upload : uploads) {
                Path path = storageService.resolve(upload.getStoredPath());
                if (!Files.exists(path)) {
                    continue;
                }
                zos.putNextEntry(new ZipEntry(upload.getId() + "-" + sanitizeForZipEntry(upload.getOriginalFilename())));
                Files.copy(path, zos);
                zos.closeEntry();
            }
        }
    }

    /** Strips any directory components and control characters from a user-supplied filename before it becomes a zip entry name. */
    private String sanitizeForZipEntry(String filename) {
        String cleaned = StringUtils.cleanPath(StringUtils.hasText(filename) ? filename : "datei");
        int lastSlash = Math.max(cleaned.lastIndexOf('/'), cleaned.lastIndexOf('\\'));
        String name = lastSlash >= 0 ? cleaned.substring(lastSlash + 1) : cleaned;
        return name.replaceAll("[\\x00-\\x1f]", "");
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        fanUploadService.delete(id);
        redirectAttributes.addFlashAttribute("success", "Datei wurde gelöscht.");
        return "redirect:/admin/fan-uploads";
    }
}
