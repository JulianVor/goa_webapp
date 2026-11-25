package de.goafestival.webapp.dto;

import de.goafestival.webapp.domain.EditionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

/** Backing bean for the admin "create/edit edition" form. */
public class EditionForm {

    private Long id;

    @NotNull
    private EditionType type = EditionType.FESTIVAL;

    /** Required for FESTIVAL; for KNEIPENKONZERT it's derived from startDate instead (see EditionService). */
    private Integer year;

    /** Required for FESTIVAL; not shown for KNEIPENKONZERT, where the year isn't relevant (see EditionService). */
    private String displayLabel;

    @NotBlank
    private String title;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    private Long locationId;

    private String aboutText;

    /** The "true" primary/brand color (e.g. Bordeaux red for 2026) - not yet used anywhere in rendering. */
    private String colorPrimary;
    @NotBlank
    private String colorSecondary = "#2f6f68";
    @NotBlank
    private String colorAccent2 = "#e0559a";

    @NotBlank
    private String colorBox = "#2f6f68";
    @NotBlank
    private String colorAccentBox = "#2f6f68";
    @NotBlank
    private String colorHeading = "#f2c14e";
    @NotBlank
    private String colorButton = "#f2c14e";
    @NotBlank
    private String colorHeadingOutline = "#2f6f68";
    @NotBlank
    private String colorText = "#f2c14e";

    @NotNull
    private Integer boxRadius = 20;
    @NotNull
    private Integer pillRadius = 50;

    @NotBlank
    private String headingFont = "BRUGTY";

    private boolean current;

    private boolean showEventInfos = true;
    private boolean showLineup = true;
    private boolean showFaq = true;
    private boolean showHeadliner = true;
    private boolean showHallOfFame = false;

    private MultipartFile logoImage;
    private MultipartFile backgroundImage;
    private MultipartFile locationImage;

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

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public String getAboutText() {
        return aboutText;
    }

    public void setAboutText(String aboutText) {
        this.aboutText = aboutText;
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

    public String getColorBox() {
        return colorBox;
    }

    public void setColorBox(String colorBox) {
        this.colorBox = colorBox;
    }

    public String getColorAccentBox() {
        return colorAccentBox;
    }

    public void setColorAccentBox(String colorAccentBox) {
        this.colorAccentBox = colorAccentBox;
    }

    public String getColorHeading() {
        return colorHeading;
    }

    public void setColorHeading(String colorHeading) {
        this.colorHeading = colorHeading;
    }

    public String getColorButton() {
        return colorButton;
    }

    public void setColorButton(String colorButton) {
        this.colorButton = colorButton;
    }

    public String getColorHeadingOutline() {
        return colorHeadingOutline;
    }

    public void setColorHeadingOutline(String colorHeadingOutline) {
        this.colorHeadingOutline = colorHeadingOutline;
    }

    public String getColorText() {
        return colorText;
    }

    public void setColorText(String colorText) {
        this.colorText = colorText;
    }

    public Integer getBoxRadius() {
        return boxRadius;
    }

    public void setBoxRadius(Integer boxRadius) {
        this.boxRadius = boxRadius;
    }

    public Integer getPillRadius() {
        return pillRadius;
    }

    public void setPillRadius(Integer pillRadius) {
        this.pillRadius = pillRadius;
    }

    public String getHeadingFont() {
        return headingFont;
    }

    public void setHeadingFont(String headingFont) {
        this.headingFont = headingFont;
    }

    public boolean isCurrent() {
        return current;
    }

    public void setCurrent(boolean current) {
        this.current = current;
    }

    public boolean isShowEventInfos() {
        return showEventInfos;
    }

    public void setShowEventInfos(boolean showEventInfos) {
        this.showEventInfos = showEventInfos;
    }

    public boolean isShowLineup() {
        return showLineup;
    }

    public void setShowLineup(boolean showLineup) {
        this.showLineup = showLineup;
    }

    public boolean isShowFaq() {
        return showFaq;
    }

    public void setShowFaq(boolean showFaq) {
        this.showFaq = showFaq;
    }

    public boolean isShowHeadliner() {
        return showHeadliner;
    }

    public void setShowHeadliner(boolean showHeadliner) {
        this.showHeadliner = showHeadliner;
    }

    public boolean isShowHallOfFame() {
        return showHallOfFame;
    }

    public void setShowHallOfFame(boolean showHallOfFame) {
        this.showHallOfFame = showHallOfFame;
    }

    public MultipartFile getLogoImage() {
        return logoImage;
    }

    public void setLogoImage(MultipartFile logoImage) {
        this.logoImage = logoImage;
    }

    public MultipartFile getBackgroundImage() {
        return backgroundImage;
    }

    public void setBackgroundImage(MultipartFile backgroundImage) {
        this.backgroundImage = backgroundImage;
    }

    public MultipartFile getLocationImage() {
        return locationImage;
    }

    public void setLocationImage(MultipartFile locationImage) {
        this.locationImage = locationImage;
    }
}
