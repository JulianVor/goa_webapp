package de.goafestival.webapp.web.admin;

import de.goafestival.webapp.service.export.DataExportImportService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Full-site backup/restore: every DB row plus every uploaded image, as one .zip. */
@Controller
@RequestMapping("/admin/data")
public class AdminDataController {

    private final DataExportImportService dataExportImportService;

    public AdminDataController(DataExportImportService dataExportImportService) {
        this.dataExportImportService = dataExportImportService;
    }

    @GetMapping
    public String page() {
        return "admin/data";
    }

    @GetMapping("/export")
    public void export(HttpServletResponse response) throws IOException {
        String filename = "goa-export-" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + ".zip";
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        dataExportImportService.exportTo(response.getOutputStream());
    }

    @PostMapping("/import")
    public String importData(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Bitte eine Export-Datei (.zip) auswählen.");
            return "redirect:/admin/data";
        }
        try {
            DataExportImportService.ImportResult result = dataExportImportService.importFrom(file.getInputStream());
            redirectAttributes.addFlashAttribute("success", "Import erfolgreich: " + result.summary());
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Import fehlgeschlagen: " + e.getMessage());
        }
        return "redirect:/admin/data";
    }
}
