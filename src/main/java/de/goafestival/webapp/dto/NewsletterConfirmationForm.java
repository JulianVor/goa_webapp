package de.goafestival.webapp.dto;

import jakarta.validation.constraints.NotBlank;

/** Backing bean for the admin "Bestätigungsmail" form - the template auto-sent on signup. */
public class NewsletterConfirmationForm {

    @NotBlank
    private String subject;

    @NotBlank
    private String htmlBody;

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getHtmlBody() {
        return htmlBody;
    }

    public void setHtmlBody(String htmlBody) {
        this.htmlBody = htmlBody;
    }
}
