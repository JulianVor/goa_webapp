package de.goafestival.webapp.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * One festival edition ("Jahr"). Holds the year-specific branding (colors, logo,
 * background), the event info shown in the hero/"Event-Infos" section, and owns
 * the bands and FAQ entries that belong to that year.
 */
@Entity
@Table(name = "editions", uniqueConstraints = @UniqueConstraint(columnNames = "festival_year"))
public class Edition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "festival_year", nullable = false)
    private Integer year;

    /** Large number/label shown in the hero graphic, e.g. "2026" or "60". */
    @Column(nullable = false)
    private String displayLabel;

    /** Festival title, e.g. "Grumbrechtstraßen Open Air". */
    @Column(nullable = false)
    private String title;

    private String headlinerName;

    /** Free-text date label shown next to the headliner, e.g. "10./11.07.26". */
    private String headlinerDateLabel;

    private LocalDate startDate;
    private LocalDate endDate;

    private String locationName;
    private String locationStreet;
    private String locationZipCity;

    @Column(length = 4000)
    private String aboutText;

    /** Address used for the Google Maps embed + "Route berechnen" link. */
    private String mapQuery;

    private String colorPrimary = "#2f6f68";
    private String colorSecondary = "#e0559a";
    private String colorAccent = "#f2c14e";
    private String colorText = "#ffffff";

    private String logoImagePath;
    private String backgroundImagePath;

    /**
     * Second hero graphic (e.g. a "Tipsy Apes 10./11.07.26" style venue+date
     * announcement image). Falls back to the headlinerName/headlinerDateLabel
     * text rendering when not set.
     */
    private String locationImagePath;

    /**
     * Per-section visibility toggles, e.g. to publish next year's logo/hero
     * before there's a line-up yet. Nullable so existing rows (created before
     * these columns existed) default to "shown" without a data migration.
     */
    private Boolean showEventInfos;
    private Boolean showLineup;
    private Boolean showFaq;
    private Boolean showHeadliner;

    /** Only one edition may be "current" (shown at "/"); others are reachable as history. */
    @Column(name = "is_current", nullable = false)
    private boolean current = false;

    @OneToMany(mappedBy = "edition", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("performanceAt ASC")
    private List<Band> bands = new ArrayList<>();

    @OneToMany(mappedBy = "edition", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    private List<FaqEntry> faqEntries = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }

    public void setDisplayLabel(String displayLabel) {
        this.displayLabel = displayLabel;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getHeadlinerName() {
        return headlinerName;
    }

    public void setHeadlinerName(String headlinerName) {
        this.headlinerName = headlinerName;
    }

    public String getHeadlinerDateLabel() {
        return headlinerDateLabel;
    }

    public void setHeadlinerDateLabel(String headlinerDateLabel) {
        this.headlinerDateLabel = headlinerDateLabel;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getLocationStreet() {
        return locationStreet;
    }

    public void setLocationStreet(String locationStreet) {
        this.locationStreet = locationStreet;
    }

    public String getLocationZipCity() {
        return locationZipCity;
    }

    public void setLocationZipCity(String locationZipCity) {
        this.locationZipCity = locationZipCity;
    }

    public String getAboutText() {
        return aboutText;
    }

    public void setAboutText(String aboutText) {
        this.aboutText = aboutText;
    }

    public String getMapQuery() {
        return mapQuery;
    }

    public void setMapQuery(String mapQuery) {
        this.mapQuery = mapQuery;
    }

    public String getColorPrimary() {
        return colorPrimary;
    }

    public void setColorPrimary(String colorPrimary) {
        this.colorPrimary = colorPrimary;
    }

    public String getColorSecondary() {
        return colorSecondary;
    }

    public void setColorSecondary(String colorSecondary) {
        this.colorSecondary = colorSecondary;
    }

    public String getColorAccent() {
        return colorAccent;
    }

    public void setColorAccent(String colorAccent) {
        this.colorAccent = colorAccent;
    }

    public String getColorText() {
        return colorText;
    }

    public void setColorText(String colorText) {
        this.colorText = colorText;
    }

    public String getLogoImagePath() {
        return logoImagePath;
    }

    public void setLogoImagePath(String logoImagePath) {
        this.logoImagePath = logoImagePath;
    }

    public String getBackgroundImagePath() {
        return backgroundImagePath;
    }

    public void setBackgroundImagePath(String backgroundImagePath) {
        this.backgroundImagePath = backgroundImagePath;
    }

    public String getLocationImagePath() {
        return locationImagePath;
    }

    public void setLocationImagePath(String locationImagePath) {
        this.locationImagePath = locationImagePath;
    }

    public boolean isShowEventInfos() {
        return showEventInfos == null || showEventInfos;
    }

    public void setShowEventInfos(Boolean showEventInfos) {
        this.showEventInfos = showEventInfos;
    }

    public boolean isShowLineup() {
        return showLineup == null || showLineup;
    }

    public void setShowLineup(Boolean showLineup) {
        this.showLineup = showLineup;
    }

    public boolean isShowFaq() {
        return showFaq == null || showFaq;
    }

    public void setShowFaq(Boolean showFaq) {
        this.showFaq = showFaq;
    }

    public boolean isShowHeadliner() {
        return showHeadliner == null || showHeadliner;
    }

    public void setShowHeadliner(Boolean showHeadliner) {
        this.showHeadliner = showHeadliner;
    }

    public boolean isCurrent() {
        return current;
    }

    public void setCurrent(boolean current) {
        this.current = current;
    }

    public List<Band> getBands() {
        return bands;
    }

    public void setBands(List<Band> bands) {
        this.bands = bands;
    }

    public List<FaqEntry> getFaqEntries() {
        return faqEntries;
    }

    public void setFaqEntries(List<FaqEntry> faqEntries) {
        this.faqEntries = faqEntries;
    }
}
