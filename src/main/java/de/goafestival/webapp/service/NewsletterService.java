package de.goafestival.webapp.service;

import de.goafestival.webapp.domain.Edition;
import de.goafestival.webapp.domain.NewsletterSubscriber;
import de.goafestival.webapp.domain.SiteSettings;
import de.goafestival.webapp.repository.NewsletterSubscriberRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class NewsletterService {

    private static final Logger log = LoggerFactory.getLogger(NewsletterService.class);

    private static final String DEFAULT_CONFIRMATION_SUBJECT = "Newsletter-Anmeldung bestätigt";
    private static final String DEFAULT_CONFIRMATION_BODY =
            "<p>Hallo,</p><p>danke für deine Anmeldung zum Newsletter! Du bekommst ab jetzt Neuigkeiten und "
                    + "Updates rund ums Festival direkt in dein Postfach.</p>";

    private final NewsletterSubscriberRepository subscriberRepository;
    private final JavaMailSender mailSender;
    private final EditionService editionService;
    private final SiteSettingsService siteSettingsService;
    private final ITemplateEngine templateEngine;
    private final String fromAddress;
    private final String configuredBaseUrl;
    private final boolean enabled;

    public NewsletterService(NewsletterSubscriberRepository subscriberRepository, JavaMailSender mailSender,
                              EditionService editionService, SiteSettingsService siteSettingsService,
                              ITemplateEngine templateEngine,
                              @Value("${app.mail.from:}") String configuredFrom,
                              @Value("${spring.mail.username:}") String mailUsername,
                              @Value("${spring.mail.host:}") String mailHost,
                              @Value("${app.base-url:}") String configuredBaseUrl) {
        this.subscriberRepository = subscriberRepository;
        this.mailSender = mailSender;
        this.editionService = editionService;
        this.siteSettingsService = siteSettingsService;
        this.templateEngine = templateEngine;
        this.fromAddress = !configuredFrom.isBlank() ? configuredFrom
                : !mailUsername.isBlank() ? mailUsername
                : "newsletter@goa-festival.de";
        this.configuredBaseUrl = configuredBaseUrl.isBlank() ? null : stripTrailingSlash(configuredBaseUrl);
        this.enabled = !mailHost.isBlank();
    }

    /** Whether an SMTP server is configured at all - the whole feature is hidden/rejected otherwise. */
    public boolean isEnabled() {
        return enabled;
    }

    public List<NewsletterSubscriber> findAllOrdered() {
        return subscriberRepository.findAllByOrderBySubscribedAtDesc();
    }

    /** Stores a new subscriber. Rejects an email that's already signed up, or if no mail server is configured. */
    public void subscribe(String email) {
        if (!enabled) {
            throw new IllegalStateException("Der Newsletter ist aktuell nicht verfügbar.");
        }
        String normalized = email.trim().toLowerCase();
        if (subscriberRepository.existsByEmailIgnoreCase(normalized)) {
            throw new IllegalStateException("Diese E-Mail-Adresse ist bereits für den Newsletter angemeldet.");
        }
        NewsletterSubscriber subscriber = new NewsletterSubscriber();
        subscriber.setEmail(normalized);
        subscriber.setUnsubscribeToken(UUID.randomUUID().toString());
        subscriberRepository.save(subscriber);
        sendConfirmation(subscriber);
    }

    /** The admin-configured confirmation subject/body, or a sensible default if nothing was saved yet. */
    public String getConfirmationSubject() {
        String subject = siteSettingsService.get().getNewsletterConfirmationSubject();
        return (subject != null && !subject.isBlank()) ? subject : DEFAULT_CONFIRMATION_SUBJECT;
    }

    public String getConfirmationBody() {
        String body = siteSettingsService.get().getNewsletterConfirmationBody();
        return (body != null && !body.isBlank()) ? body : DEFAULT_CONFIRMATION_BODY;
    }

    public void updateConfirmationTemplate(String subject, String body) {
        siteSettingsService.updateNewsletterConfirmation(subject, body);
    }

    /**
     * Sent right after a successful signup, in the same branded layout as a regular
     * newsletter. A failure here must never undo the signup itself, so it's just logged.
     */
    private void sendConfirmation(NewsletterSubscriber subscriber) {
        try {
            String baseUrl = resolveBaseUrl();
            String unsubscribeUrl = baseUrl + "/newsletter/unsubscribe?token=" + subscriber.getUnsubscribeToken();
            String fullHtml = renderEmailHtml(getConfirmationBody(), unsubscribeUrl, baseUrl);
            send(subscriber, getConfirmationSubject(), fullHtml);
        } catch (MessagingException | MailException e) {
            log.warn("Newsletter-Bestätigungsmail an {} konnte nicht gesendet werden", subscriber.getEmail(), e);
        }
    }

    /** No-op (not an error) for an already-used or unknown token, since this is a public email link. */
    public void unsubscribeByToken(String token) {
        subscriberRepository.findByUnsubscribeToken(token).ifPresent(subscriberRepository::delete);
    }

    public void delete(Long id) {
        subscriberRepository.deleteById(id);
    }

    /**
     * Sends one HTML mail per subscriber (never a shared To/Cc/Bcc list, so
     * subscribers never see each other's address), each with its own unsubscribe
     * link appended. Runs in the calling (admin request) thread; fine for a
     * mailing list this size, but a large one would need this moved off-thread.
     * Returns how many were sent before a failure, if any (surfaced by the caller).
     */
    public int sendNewsletter(String subject, String htmlBody) {
        if (!enabled) {
            throw new IllegalStateException("Der Newsletter ist aktuell nicht verfügbar.");
        }
        String baseUrl = resolveBaseUrl();
        int sent = 0;
        for (NewsletterSubscriber subscriber : findAllOrdered()) {
            try {
                String unsubscribeUrl = baseUrl + "/newsletter/unsubscribe?token=" + subscriber.getUnsubscribeToken();
                String fullHtml = renderEmailHtml(htmlBody, unsubscribeUrl, baseUrl);
                send(subscriber, subject, fullHtml);
                sent++;
            } catch (MessagingException | MailException e) {
                throw new NewsletterSendException(subscriber.getEmail(), sent, e);
            }
        }
        return sent;
    }

    /** Renders the exact branded HTML that {@link #sendNewsletter} would send, for the admin's preview step. */
    public String previewHtml(String htmlBody) {
        String baseUrl = resolveBaseUrl();
        String unsubscribeUrl = baseUrl + "/newsletter/unsubscribe?token=vorschau";
        return renderEmailHtml(htmlBody, unsubscribeUrl, baseUrl);
    }

    /**
     * The public URL images/links in the mail must use. Prefers the explicitly configured
     * {@code APP_BASE_URL} - the admin's own request (reverse proxy, internal network, plain
     * HTTP) may not be the address recipients' mail clients can actually reach - and falls
     * back to the current request's URL, which is fine when the admin panel is reached at
     * the same public address the site itself is.
     */
    private String resolveBaseUrl() {
        if (configuredBaseUrl != null) {
            return configuredBaseUrl;
        }
        return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    /**
     * Wraps the admin-authored content in the branded email layout (logo, current
     * edition's colors, legal unsubscribe footer). Colors are inlined as literal hex
     * values rather than CSS custom properties, since most email clients strip
     * {@code <style>} blocks and don't support {@code var(...)} at all.
     */
    private String renderEmailHtml(String contentHtml, String unsubscribeUrl, String baseUrl) {
        Edition edition = editionService.findCurrent().orElse(null);
        SiteSettings siteSettings = siteSettingsService.get();
        String logoPath = siteSettings.getLogoImagePath();

        Context context = new Context();
        context.setVariable("contentHtml", contentHtml);
        context.setVariable("unsubscribeUrl", unsubscribeUrl);
        context.setVariable("colorPrimary", edition != null ? edition.getColorPrimary() : "#2f6f68");
        context.setVariable("colorSecondary", edition != null ? edition.getColorSecondary() : "#e0559a");
        context.setVariable("colorAccent", edition != null ? edition.getColorAccent() : "#f2c14e");
        context.setVariable("editionTitle", edition != null ? edition.getTitle() : "Newsletter");
        context.setVariable("logoUrl", (logoPath != null && !logoPath.isBlank()) ? baseUrl + logoPath : null);
        String backgroundPath = edition != null ? edition.getBackgroundImagePath() : null;
        context.setVariable("backgroundUrl", (backgroundPath != null && !backgroundPath.isBlank()) ? baseUrl + backgroundPath : null);
        return templateEngine.process("email/newsletter-email", context);
    }

    private void send(NewsletterSubscriber subscriber, String subject, String fullHtml) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
        helper.setTo(subscriber.getEmail());
        helper.setFrom(fromAddress);
        helper.setSubject(subject);
        helper.setText(fullHtml, true);
        mailSender.send(message);
    }

    /** Thrown mid-send so the admin sees exactly how many mails went out before the failure. */
    public static class NewsletterSendException extends RuntimeException {
        private final int sentBeforeFailure;

        public NewsletterSendException(String failedEmail, int sentBeforeFailure, Throwable cause) {
            super("Newsletter konnte nicht an " + failedEmail + " gesendet werden.", cause);
            this.sentBeforeFailure = sentBeforeFailure;
        }

        public int getSentBeforeFailure() {
            return sentBeforeFailure;
        }
    }
}
