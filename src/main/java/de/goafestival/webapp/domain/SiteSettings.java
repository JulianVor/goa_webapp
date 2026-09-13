package de.goafestival.webapp.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Single global row (fixed id) holding branding and contact info that is
 * independent of any one festival edition: the nav/toolbar logo, the browser
 * tab icon, social/contact links and the Impressum.
 */
@Entity
@Table(name = "site_settings")
public class SiteSettings {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    private String logoImagePath;
    private String faviconImagePath;

    private String instagramUrl;
    private String facebookUrl;
    private String contactEmail;

    /** Rendered as raw HTML on the Impressum page, so admins can format it freely. */
    @Column(columnDefinition = "TEXT")
    private String impressumText;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLogoImagePath() {
        return logoImagePath;
    }

    public void setLogoImagePath(String logoImagePath) {
        this.logoImagePath = logoImagePath;
    }

    public String getFaviconImagePath() {
        return faviconImagePath;
    }

    public void setFaviconImagePath(String faviconImagePath) {
        this.faviconImagePath = faviconImagePath;
    }

    public String getInstagramUrl() {
        return instagramUrl;
    }

    public void setInstagramUrl(String instagramUrl) {
        this.instagramUrl = instagramUrl;
    }

    public String getFacebookUrl() {
        return facebookUrl;
    }

    public void setFacebookUrl(String facebookUrl) {
        this.facebookUrl = facebookUrl;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getImpressumText() {
        return impressumText;
    }

    public void setImpressumText(String impressumText) {
        this.impressumText = impressumText;
    }
}
