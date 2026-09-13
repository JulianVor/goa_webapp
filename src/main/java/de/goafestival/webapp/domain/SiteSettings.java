package de.goafestival.webapp.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Single global row (fixed id) holding branding that is independent of any one
 * festival edition: the nav/toolbar logo and the browser tab icon.
 */
@Entity
@Table(name = "site_settings")
public class SiteSettings {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    private String logoImagePath;
    private String faviconImagePath;

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
}
