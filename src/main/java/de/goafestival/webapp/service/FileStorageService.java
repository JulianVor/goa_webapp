package de.goafestival.webapp.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Stores uploaded images (logos, backgrounds, band photos) on the local filesystem
 * under {@code app.upload-dir}, served back via {@code /uploads/**} (see WebConfig).
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final Path root;

    public FileStorageService(@Value("${app.upload-dir:uploads}") String uploadDir) {
        this.root = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create upload directory " + root, e);
        }
    }

    /**
     * Stores the file under the given sub-directory (e.g. "bands", "editions/logos") and
     * returns the public path (e.g. "/uploads/bands/uuid.jpg"), or {@code null} if the
     * file is empty.
     */
    public String store(MultipartFile file, String subDir) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        String extension = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) {
            extension = original.substring(dot).toLowerCase();
        }
        String filename = UUID.randomUUID() + extension;
        try {
            Path targetDir = root.resolve(subDir).normalize();
            if (!targetDir.startsWith(root)) {
                throw new IllegalArgumentException("Invalid sub directory: " + subDir);
            }
            Files.createDirectories(targetDir);
            Path target = targetDir.resolve(filename);
            file.transferTo(target);
            return "/uploads/" + subDir + "/" + filename;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store uploaded file " + original, e);
        }
    }

    /**
     * Duplicates a previously stored file under a (possibly different) sub-directory and
     * returns the new public path, or {@code null} if the source path is unset or missing.
     * Used when copying an entity (e.g. a band) between editions: each copy gets its own
     * file so deleting/replacing one side's image never affects the other's.
     */
    public String copy(String sourcePublicPath, String subDir) {
        if (!StringUtils.hasText(sourcePublicPath) || !sourcePublicPath.startsWith("/uploads/")) {
            return null;
        }
        Path source = root.resolve(sourcePublicPath.substring("/uploads/".length())).normalize();
        if (!source.startsWith(root) || !Files.exists(source)) {
            return null;
        }
        String original = source.getFileName().toString();
        String extension = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) {
            extension = original.substring(dot).toLowerCase();
        }
        String filename = UUID.randomUUID() + extension;
        try {
            Path targetDir = root.resolve(subDir).normalize();
            if (!targetDir.startsWith(root)) {
                throw new IllegalArgumentException("Invalid sub directory: " + subDir);
            }
            Files.createDirectories(targetDir);
            Path target = targetDir.resolve(filename);
            Files.copy(source, target);
            return "/uploads/" + subDir + "/" + filename;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to copy file " + source, e);
        }
    }

    /** Best-effort delete of a previously stored file, identified by its public path. */
    public void delete(String publicPath) {
        if (!StringUtils.hasText(publicPath) || !publicPath.startsWith("/uploads/")) {
            return;
        }
        Path target = root.resolve(publicPath.substring("/uploads/".length())).normalize();
        if (!target.startsWith(root)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            log.warn("Could not delete file {}", target, e);
        }
    }
}
