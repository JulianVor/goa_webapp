package de.goafestival.webapp.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

/**
 * Stores uploaded images (logos, backgrounds, band photos) on the local filesystem
 * under {@code app.upload-dir}, served back via {@code /uploads/**} (see WebConfig).
 *
 * <p>Raster images are downscaled and re-compressed on upload so a phone-camera
 * original (often several MB, several thousand pixels wide) doesn't get shipped at
 * full size to every visitor just to be displayed in a small card or a background
 * that's scaled down by CSS anyway.
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private static final Set<String> RASTER_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp");
    private static final float JPEG_QUALITY = 0.82f;

    /** Max dimension (longer edge, px) presets for {@link #store(MultipartFile, String, int)}. */
    public static final int MAX_DIMENSION_BACKGROUND = 2400;
    public static final int MAX_DIMENSION_STANDARD = 1400;
    public static final int MAX_DIMENSION_ICON = 512;

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
     * file is empty. Raster images are downscaled to at most {@code maxDimension} on
     * their longer edge and re-compressed (opaque images as JPEG, images with
     * transparency as PNG); anything ImageIO can't decode (e.g. SVG) is stored as-is.
     */
    public String store(MultipartFile file, String subDir, int maxDimension) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        String extension = extensionOf(original);
        try {
            Path targetDir = resolveSubDir(subDir);
            if (RASTER_EXTENSIONS.contains(extension)) {
                BufferedImage image;
                try (InputStream in = file.getInputStream()) {
                    image = ImageIO.read(in);
                }
                if (image != null) {
                    return writeResized(image, targetDir, subDir, maxDimension);
                }
            }
            String filename = UUID.randomUUID() + extension;
            file.transferTo(targetDir.resolve(filename));
            return "/uploads/" + subDir + "/" + filename;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store uploaded file " + original, e);
        }
    }

    /**
     * Duplicates a previously stored file under a (possibly different) sub-directory and
     * returns the new public path, or {@code null} if the source path is unset or missing.
     * Used when copying an entity (e.g. a band) between editions: each copy gets its own
     * file so deleting/replacing one side's image never affects the other's. The source
     * was already resized on its own upload, so this is a plain copy.
     */
    public String copy(String sourcePublicPath, String subDir) {
        if (!StringUtils.hasText(sourcePublicPath) || !sourcePublicPath.startsWith("/uploads/")) {
            return null;
        }
        Path source = root.resolve(sourcePublicPath.substring("/uploads/".length())).normalize();
        if (!source.startsWith(root) || !Files.exists(source)) {
            return null;
        }
        String extension = extensionOf(source.getFileName().toString());
        try {
            Path targetDir = resolveSubDir(subDir);
            String filename = UUID.randomUUID() + extension;
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

    private String writeResized(BufferedImage image, Path targetDir, String subDir, int maxDimension) throws IOException {
        BufferedImage scaled = scaleDown(image, maxDimension);
        boolean hasAlpha = scaled.getColorModel().hasAlpha();
        String filename = UUID.randomUUID() + (hasAlpha ? ".png" : ".jpg");
        Path target = targetDir.resolve(filename);
        if (hasAlpha) {
            ImageIO.write(scaled, "png", target.toFile());
        } else {
            writeJpeg(scaled, target);
        }
        return "/uploads/" + subDir + "/" + filename;
    }

    private BufferedImage scaleDown(BufferedImage image, int maxDimension) {
        int width = image.getWidth();
        int height = image.getHeight();
        int longerEdge = Math.max(width, height);
        if (longerEdge <= maxDimension) {
            return image;
        }
        double scale = (double) maxDimension / longerEdge;
        int newWidth = Math.max(1, (int) Math.round(width * scale));
        int newHeight = Math.max(1, (int) Math.round(height * scale));
        int type = image.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage scaled = new BufferedImage(newWidth, newHeight, type);
        Graphics2D g = scaled.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(image, 0, 0, newWidth, newHeight, null);
        } finally {
            g.dispose();
        }
        return scaled;
    }

    private void writeJpeg(BufferedImage image, Path target) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        ImageWriter writer = writers.next();
        try {
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_QUALITY);
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(target.toFile())) {
                writer.setOutput(ios);
                writer.write(null, new IIOImage(image, null, null), param);
            }
        } finally {
            writer.dispose();
        }
    }

    private String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot).toLowerCase() : "";
    }

    private Path resolveSubDir(String subDir) throws IOException {
        Path targetDir = root.resolve(subDir).normalize();
        if (!targetDir.startsWith(root)) {
            throw new IllegalArgumentException("Invalid sub directory: " + subDir);
        }
        Files.createDirectories(targetDir);
        return targetDir;
    }
}
