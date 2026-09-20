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

    @NotBlank
    private String colorPrimary = "#2f6f68";
    @NotBlank
    private String colorSecondary = "#e0559a";
    @NotBlank
    private String colorAccent = "#f2c14e";

    private boolean current;

    private boolean showEventInfos = true;
    private boolean showLineup = true;
    private boolean showFaq = true;
    private boolean showHeadliner = true;

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

    public String getColorAccent() {
        return colorAccent;
    }

    public void setColorAccent(String colorAccent) {
        this.colorAccent = colorAccent;
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
