package de.goafestival.webapp.dto;

import org.springframework.web.multipart.MultipartFile;

/** Backing bean for the admin "global settings" form (nav logo + favicon). */
public class SiteSettingsForm {

    private MultipartFile logoImage;
    private MultipartFile faviconImage;

    private String instagramUrl;
    private String facebookUrl;
    private String contactEmail;
    private String impressumText;

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
