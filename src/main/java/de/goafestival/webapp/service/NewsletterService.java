package de.goafestival.webapp.service;

import de.goafestival.webapp.domain.NewsletterSubscriber;
import de.goafestival.webapp.repository.NewsletterSubscriberRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class NewsletterService {

    private final NewsletterSubscriberRepository subscriberRepository;
    private final JavaMailSender mailSender;
    private final String fromAddress;

    public NewsletterService(NewsletterSubscriberRepository subscriberRepository, JavaMailSender mailSender,
                              @Value("${app.mail.from:}") String configuredFrom,
                              @Value("${spring.mail.username:}") String mailUsername) {
        this.subscriberRepository = subscriberRepository;
        this.mailSender = mailSender;
        this.fromAddress = !configuredFrom.isBlank() ? configuredFrom
                : !mailUsername.isBlank() ? mailUsername
                : "newsletter@goa-festival.de";
    }

    public List<NewsletterSubscriber> findAllOrdered() {
        return subscriberRepository.findAllByOrderBySubscribedAtDesc();
    }

    /** Stores a new subscriber. Rejects an email that's already signed up. */
    public void subscribe(String email) {
        String normalized = email.trim().toLowerCase();
        if (subscriberRepository.existsByEmailIgnoreCase(normalized)) {
            throw new IllegalStateException("Diese E-Mail-Adresse ist bereits für den Newsletter angemeldet.");
        }
        NewsletterSubscriber subscriber = new NewsletterSubscriber();
        subscriber.setEmail(normalized);
        subscriber.setUnsubscribeToken(UUID.randomUUID().toString());
        subscriberRepository.save(subscriber);
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
        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        int sent = 0;
        for (NewsletterSubscriber subscriber : findAllOrdered()) {
            try {
                send(subscriber, subject, htmlBody, baseUrl);
                sent++;
            } catch (MessagingException | MailException e) {
                throw new NewsletterSendException(subscriber.getEmail(), sent, e);
            }
        }
        return sent;
    }

    private void send(NewsletterSubscriber subscriber, String subject, String htmlBody, String baseUrl) throws MessagingException {
        String unsubscribeUrl = baseUrl + "/newsletter/unsubscribe?token=" + subscriber.getUnsubscribeToken();
        String fullHtml = htmlBody
                + "<hr style=\"margin-top:2rem;border:none;border-top:1px solid #ccc;\"/>"
                + "<p style=\"font-size:12px;color:#888;\">Du erhältst diese E-Mail, weil du dich für den Newsletter "
                + "angemeldet hast. <a href=\"" + unsubscribeUrl + "\">Newsletter abbestellen</a></p>";

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
