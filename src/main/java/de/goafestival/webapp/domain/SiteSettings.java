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

    /** Subject/body for the auto-sent newsletter signup confirmation - null/blank until an admin configures it. */
    private String newsletterConfirmationSubject;

    @Column(columnDefinition = "TEXT")
    private String newsletterConfirmationBody;

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

    public String getNewsletterConfirmationSubject() {
        return newsletterConfirmationSubject;
    }

    public void setNewsletterConfirmationSubject(String newsletterConfirmationSubject) {
        this.newsletterConfirmationSubject = newsletterConfirmationSubject;
    }

    public String getNewsletterConfirmationBody() {
        return newsletterConfirmationBody;
    }

    public void setNewsletterConfirmationBody(String newsletterConfirmationBody) {
        this.newsletterConfirmationBody = newsletterConfirmationBody;
    }
}
