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
 * Stores fan-uploaded photos/videos on disk under {@code app.fan-upload-dir}, deliberately
 * separate from {@link FileStorageService} in two ways:
 *
 * <p>1. Files are written byte-for-byte as received, with no resizing or re-compression -
 * the whole point of this upload is to keep full original quality, unlike every other
 * image on the site (band photos, logos, ...), which are always downscaled/recompressed.
 *
 * <p>2. The storage root is <em>not</em> the {@code app.upload-dir} tree that {@code
 * WebConfig} serves publicly at {@code /uploads/**}. Fan uploads are only ever read back
 * through the admin-only streaming endpoint ({@code AdminFanUploadController}), so simply
 * not linking the upload page isn't the only thing keeping these files private.
 */
@Service
public class FanUploadStorageService {

    private static final Logger log = LoggerFactory.getLogger(FanUploadStorageService.class);

    private final Path root;

    public FanUploadStorageService(@Value("${app.fan-upload-dir:fan-uploads}") String fanUploadDir) {
        this.root = Path.of(fanUploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create fan upload directory " + root, e);
        }
    }

    /** Stores the file as-is under a random filename and returns that filename, or {@code null} if the file is empty. */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        String filename = UUID.randomUUID() + extensionOf(original);
        try {
            file.transferTo(root.resolve(filename));
            return filename;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store fan upload " + original, e);
        }
    }

    /** Resolves a stored filename to its path on disk, rejecting anything that would escape the storage root. */
    public Path resolve(String storedFilename) {
        Path target = root.resolve(storedFilename).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("Invalid fan upload path: " + storedFilename);
        }
        return target;
    }

    /** Best-effort delete of a previously stored file. */
    public void delete(String storedFilename) {
        if (!StringUtils.hasText(storedFilename)) {
            return;
        }
        try {
            Files.deleteIfExists(resolve(storedFilename));
        } catch (IOException e) {
            log.warn("Could not delete fan upload {}", storedFilename, e);
        }
    }

    private String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot).toLowerCase() : "";
    }
}
