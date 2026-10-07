package de.goafestival.webapp.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * One photo or video a festival visitor uploaded for a given {@link Edition}, via the
 * unlisted "/upload" page - not linked anywhere in the site's own navigation, only
 * reachable by direct link. Stored byte-for-byte as uploaded (see
 * {@code FanUploadStorageService}, deliberately separate from the site's own
 * {@code FileStorageService}, which always downscales/recompresses) and, unlike every
 * other uploaded file on the site, never served under {@code /uploads/**}: it's only
 * reachable through the admin-only streaming endpoint, so simply not linking this page
 * isn't the only thing keeping it private.
 */
@Entity
@Table(name = "fan_uploads")
public class FanUpload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "edition_id", nullable = false)
    private Edition edition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FanMediaType mediaType;

    @Column(nullable = false)
    private String originalFilename;

    /** Filename under the fan-upload storage root (see FanUploadStorageService) - a random UUID, not user input. */
    @Column(nullable = false)
    private String storedPath;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private long fileSizeBytes;

    @Column(nullable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Edition getEdition() {
        return edition;
    }

    public void setEdition(Edition edition) {
        this.edition = edition;
    }

    public FanMediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(FanMediaType mediaType) {
        this.mediaType = mediaType;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getStoredPath() {
        return storedPath;
    }

    public void setStoredPath(String storedPath) {
        this.storedPath = storedPath;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    /** Human-readable size for the admin grid, e.g. "420 KB", "248 MB" or "1.3 GB". */
    public String getFileSizeLabel() {
        double kb = fileSizeBytes / 1024.0;
        if (kb < 1024) {
            return String.format(Locale.GERMAN, "%.0f KB", kb);
        }
        double mb = kb / 1024.0;
        if (mb >= 1024) {
            return String.format(Locale.GERMAN, "%.1f GB", mb / 1024);
        }
        return String.format(Locale.GERMAN, "%.0f MB", mb);
    }
}
