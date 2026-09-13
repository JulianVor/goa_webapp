package de.goafestival.webapp.dto;

import org.springframework.web.multipart.MultipartFile;

/** Backing bean for the admin "global settings" form (nav logo + favicon). */
public class SiteSettingsForm {

    private MultipartFile logoImage;
    private MultipartFile faviconImage;

    public MultipartFile getLogoImage() {
        return logoImage;
    }

    public void setLogoImage(MultipartFile logoImage) {
        this.logoImage = logoImage;
    }

    public MultipartFile getFaviconImage() {
        return faviconImage;
    }

    public void setFaviconImage(MultipartFile faviconImage) {
        this.faviconImage = faviconImage;
    }
}
