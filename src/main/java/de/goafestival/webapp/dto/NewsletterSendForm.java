package de.goafestival.webapp.dto;

import jakarta.validation.constraints.NotBlank;

/** Backing bean for the admin "Newsletter versenden" form. */
public class NewsletterSendForm {

    @NotBlank
    private String subject;

    /** Raw HTML, sent as-is (plus an appended per-recipient unsubscribe link). */
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
