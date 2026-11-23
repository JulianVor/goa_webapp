package de.goafestival.webapp.domain;

import de.goafestival.webapp.service.GermanDateFormats;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * One festival edition ("Jahr") or, since {@link #type} was added, one small
 * "Kneipenkonzert" pub show. Holds the branding (colors, logo, background),
 * the event info shown in the hero/"Event-Infos" section, and owns the bands
 * and FAQ entries that belong to it. The festival year is unique only among
 * FESTIVAL-type rows: several Kneipenkonzerte can share the same year (there's
 * no DB-level constraint enforcing this beyond what the service layer checks).
 */
@Entity
@Table(name = "editions")
public class Edition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EditionType type = EditionType.FESTIVAL;

    @Column(name = "festival_year", nullable = false)
    private Integer year;

    /** Large number/label shown in the hero graphic, e.g. "2026" or "60". */
    @Column(nullable = false)
    private String displayLabel;

    /** Festival title, e.g. "Grumbrechtstraßen Open Air". */
    @Column(nullable = false)
    private String title;

    private LocalDate startDate;
    private LocalDate endDate;

    /** The venue. Nullable so a brand new edition can be saved before one's been picked. */
    @ManyToOne
    @JoinColumn(name = "location_id")
    private Location location;

    @Column(length = 4000)
    private String aboutText;

    /** The "true" primary/brand color (e.g. Bordeaux red for 2026) - not yet used anywhere in rendering. */
    private String colorPrimary;
    private String colorSecondary = "#2f6f68";
    /** "Akzentfarbe" in the admin form - the field name stays colorAccent2 for DB compatibility. */
    private String colorAccent2 = "#e0559a";

    /**
     * Background of content boxes - the panel behind a band's name/genre/herkunft/
     * description, the "Was/Wann/Wo" info panel, FAQ items. Nullable so existing rows
     * fall back to the pre-this-feature look (same as colorSecondary) via the getter.
     */
    private String colorBox;

    /**
     * Background of "accent" boxes: nav bar, buttons, the headliner box, modals and
     * similar callouts - visually distinct from {@link #colorBox}'s plainer content
     * panels. Nullable, falls back like {@link #colorBox}.
     */
    private String colorAccentBox;

    /** Fill color for all headings/titles (and the label text on accent boxes/buttons). */
    private String colorHeading;

    /** Outline/text-stroke color for headings, and the border on accent boxes. */
    private String colorHeadingOutline;

    /** Default body text color. */
    private String colorText;

    /**
     * Corner rounding (px) for content/accent boxes (band detail card, info panel, FAQ
     * items, modal, headliner box, Kneipenkonzert teaser/dropdown menu) - not pill-shaped
     * buttons, which have their own {@link #pillRadius}. Nullable, falls back to 20
     * (today's de-facto default) via the getter.
     */
    private Integer boxRadius;

    /**
     * Corner rounding (px) for CTA buttons/nav pills (nav links, newsletter button,
     * "Hall of Fame"/"Teilen-Karte"/route buttons, etc.) - separate from
     * {@link #boxRadius} since these are a different ("pill") shape language. Nullable,
     * falls back to 50 via the getter - well past half the height of any of these
     * buttons, so CSS's own radius-clamping keeps them fully rounded exactly like the
     * fixed 999px they used before this field existed.
     */
    private Integer pillRadius;

    /**
     * Which display font this edition's headings/body text use. Nullable so existing
     * rows (created before this column existed) fall back to "BRUGTY" via
     * {@link #getHeadingFont()} without a data migration, the same approach as the
     * showXyz flags above.
     */
    private String headingFont;

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

    public EditionType getType() {
        return type;
    }

    public void setType(EditionType type) {
        this.type = type;
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

    /** The venue's name, e.g. "Tipsy Apes" - shown in the hero headliner box. */
    public String getHeadlinerName() {
        return location != null ? location.getName() : null;
    }

    /** Short date label for the hero headliner box, derived from start/end date. */
    public String getHeadlinerDateLabel() {
        return GermanDateFormats.headlinerDateLabel(startDate, endDate);
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

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public String getLocationName() {
        return location != null ? location.getName() : null;
    }

    public String getLocationStreet() {
        return location != null ? location.getStreet() : null;
    }

    public String getLocationZipCity() {
        return location != null ? location.getZipCity() : null;
    }

    public String getAboutText() {
        return aboutText;
    }

    public void setAboutText(String aboutText) {
        this.aboutText = aboutText;
    }

    /** Address for the Google Maps embed + "Route berechnen" link, composed from street and zip/city. */
    public String getMapQuery() {
        return location != null ? location.getMapQuery() : null;
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

    public String getColorAccent2() {
        return colorAccent2;
    }

    public void setColorAccent2(String colorAccent2) {
        this.colorAccent2 = colorAccent2;
    }

    /** Falls back to colorSecondary, matching this edition's look before colorBox existed. */
    public String getColorBox() {
        return colorBox != null ? colorBox : colorSecondary;
    }

    public void setColorBox(String colorBox) {
        this.colorBox = colorBox;
    }

    /** Falls back to colorSecondary, matching this edition's look before colorAccentBox existed. */
    public String getColorAccentBox() {
        return colorAccentBox != null ? colorAccentBox : colorSecondary;
    }

    public void setColorAccentBox(String colorAccentBox) {
        this.colorAccentBox = colorAccentBox;
    }

    /** Falls back to colorAccent2, matching this edition's look before colorHeading existed. */
    public String getColorHeading() {
        return colorHeading != null ? colorHeading : colorAccent2;
    }

    public void setColorHeading(String colorHeading) {
        this.colorHeading = colorHeading;
    }

    /** Falls back to colorSecondary, matching this edition's look before colorHeadingOutline existed. */
    public String getColorHeadingOutline() {
        return colorHeadingOutline != null ? colorHeadingOutline : colorSecondary;
    }

    public void setColorHeadingOutline(String colorHeadingOutline) {
        this.colorHeadingOutline = colorHeadingOutline;
    }

    /** Falls back to colorAccent2, matching this edition's look before colorText existed. */
    public String getColorText() {
        return colorText != null ? colorText : colorAccent2;
    }

    public void setColorText(String colorText) {
        this.colorText = colorText;
    }

    /** Never null - falls back to 20px, matching the look before this field existed. */
    public Integer getBoxRadius() {
        return boxRadius != null ? boxRadius : 20;
    }

    public void setBoxRadius(Integer boxRadius) {
        this.boxRadius = boxRadius;
    }

    /** Never null - falls back to 50px, matching the fully-rounded look before this field existed. */
    public Integer getPillRadius() {
        return pillRadius != null ? pillRadius : 50;
    }

    public void setPillRadius(Integer pillRadius) {
        this.pillRadius = pillRadius;
    }

    /** "BRUGTY" (default) or "BEBAS_NEUE" - never null, so templates can compare it directly. */
    public String getHeadingFont() {
        return headingFont == null ? "BRUGTY" : headingFont;
    }

    public void setHeadingFont(String headingFont) {
        this.headingFont = headingFont;
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

    /**
     * Whether this edition gets the full "current festival" treatment (event
     * info, FAQ, headliner box, exact performance times) rather than the bare
     * logo-plus-Line-Up view used for archived festival years. True for the
     * current festival edition and for every Kneipenkonzert (each one is a
     * standalone page, never just a dated-out archive entry).
     */
    public boolean isDetailPage() {
        return type == EditionType.KNEIPENKONZERT || current;
    }

    /**
     * An archived (non-current) festival edition is a past event: it always shows
     * just the logo and the Line-Up, regardless of the stored toggles, so these
     * three ignore the toggle once the edition is archived.
     */
    public boolean isShowEventInfos() {
        return isDetailPage() && (showEventInfos == null || showEventInfos);
    }

    public void setShowEventInfos(Boolean showEventInfos) {
        this.showEventInfos = showEventInfos;
    }

    /** The Line-Up always shows on an archived edition, regardless of the stored toggle. */
    public boolean isShowLineup() {
        return !isDetailPage() || showLineup == null || showLineup;
    }

    public void setShowLineup(Boolean showLineup) {
        this.showLineup = showLineup;
    }

    public boolean isShowFaq() {
        return isDetailPage() && (showFaq == null || showFaq);
    }

    public void setShowFaq(Boolean showFaq) {
        this.showFaq = showFaq;
    }

    public boolean isShowHeadliner() {
        return isDetailPage() && (showHeadliner == null || showHeadliner);
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
