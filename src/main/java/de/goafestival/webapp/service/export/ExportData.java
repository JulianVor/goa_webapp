package de.goafestival.webapp.service.export;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * The full content of a data export, one-to-one with the DB tables (flat, not
 * nested) so import can insert each list straight back with its original ids.
 * Plain public fields - this is an internal JSON transfer shape, not an API.
 */
public class ExportData {

    public String exportedAt;

    public List<LocationDto> locations = new ArrayList<>();
    public List<EditionDto> editions = new ArrayList<>();
    public List<BandDto> bands = new ArrayList<>();
    public List<FaqEntryDto> faqEntries = new ArrayList<>();
    public List<NewsletterSubscriberDto> newsletterSubscribers = new ArrayList<>();
    public SiteSettingsDto siteSettings;

    public static class LocationDto {
        public Long id;
        public String name;
        public String street;
        public String zipCity;
    }

    public static class EditionDto {
        public Long id;
        public String type;
        public Integer year;
        public String displayLabel;
        public String title;
        public LocalDate startDate;
        public LocalDate endDate;
        public Long locationId;
        public String aboutText;
        public String colorPrimary;
        public String colorSecondary;
        public String colorAccent2;
        public String colorAccent;
        public String logoImagePath;
        public String backgroundImagePath;
        public String locationImagePath;
        public Boolean showEventInfos;
        public Boolean showLineup;
        public Boolean showFaq;
        public Boolean showHeadliner;
        public boolean current;
    }

    public static class BandDto {
        public Long id;
        public Long editionId;
        public String name;
        public String genre;
        public String herkunft;
        public String description;
        public LocalDateTime performanceAt;
        public String mainImagePath;
        public String youtubeVideoUrl;
        public String websiteUrl;
        public String spotifyUrl;
        public String instagramUrl;
        public String facebookUrl;
        public String youtubeUrl;
        public List<String> galleryImages = new ArrayList<>();
    }

    public static class FaqEntryDto {
        public Long id;
        public Long editionId;
        public String question;
        public String answer;
        public int sortOrder;
    }

    public static class NewsletterSubscriberDto {
        public Long id;
        public String email;
        public LocalDateTime subscribedAt;
        public String unsubscribeToken;
    }

    public static class SiteSettingsDto {
        public String logoImagePath;
        public String faviconImagePath;
        public String instagramUrl;
        public String facebookUrl;
        public String contactEmail;
        public String impressumText;
        public String newsletterConfirmationSubject;
        public String newsletterConfirmationBody;
    }
}
