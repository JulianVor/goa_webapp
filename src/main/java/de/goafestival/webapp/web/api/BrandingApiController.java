package de.goafestival.webapp.web.api;

import de.goafestival.webapp.domain.Edition;
import de.goafestival.webapp.domain.SiteSettings;
import de.goafestival.webapp.service.EditionService;
import de.goafestival.webapp.service.SiteSettingsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Public, read-only branding data for other apps that want to render in this site's
 * current look without their own copy of the database - currently only goa_webapp_shop,
 * which fetches this at startup/periodically instead of depending on a deploy-time static
 * asset sync for anything that can change per edition (colors, fonts, background, logo).
 */
@RestController
@RequestMapping("/api")
public class BrandingApiController {

    private final EditionService editionService;
    private final SiteSettingsService siteSettingsService;
    private final String configuredBaseUrl;

    public BrandingApiController(EditionService editionService, SiteSettingsService siteSettingsService,
                                  @Value("${app.base-url:}") String configuredBaseUrl) {
        this.editionService = editionService;
        this.siteSettingsService = siteSettingsService;
        this.configuredBaseUrl = configuredBaseUrl.isBlank() ? null : stripTrailingSlash(configuredBaseUrl);
    }

    @GetMapping("/branding")
    public BrandingResponse branding(HttpServletRequest request) {
        Edition edition = editionService.findCurrent().orElse(null);
        SiteSettings siteSettings = siteSettingsService.get();
        String baseUrl = resolveBaseUrl(request);

        boolean bebas = edition != null && "BEBAS_NEUE".equals(edition.getHeadingFont());

        return new BrandingResponse(
                edition != null ? edition.getTitle() : "GOA Festival",
                toAbsoluteUrl(baseUrl, siteSettings.getLogoImagePath()),
                toAbsoluteUrl(baseUrl, siteSettings.getFaviconImagePath()),
                edition != null ? toAbsoluteUrl(baseUrl, edition.getBackgroundImagePath()) : null,
                edition != null ? edition.getColorSecondary() : null,
                edition != null ? edition.getColorAccent2() : null,
                edition != null ? edition.getColorAccent2() : null,
                edition != null ? edition.getColorBox() : null,
                edition != null ? edition.getColorAccentBox() : null,
                edition != null ? edition.getColorHeading() : null,
                edition != null ? edition.getColorButton() : null,
                edition != null ? edition.getColorHeadingOutline() : null,
                edition != null ? edition.getColorText() : null,
                edition != null && edition.getBoxRadius() != null ? edition.getBoxRadius() + "px" : null,
                edition != null && edition.getPillRadius() != null ? edition.getPillRadius() + "px" : null,
                bebas ? "var(--font-stack-bebas)" : "var(--font-stack-brugty)",
                bebas ? "var(--font-stack-oswald)" : "var(--font-stack-poppins)",
                bebas ? "bold" : "normal",
                "normal");
    }

    private String toAbsoluteUrl(String baseUrl, String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        return baseUrl + (path.startsWith("/") ? path : "/" + path);
    }

    /**
     * Prefers the explicitly configured {@code APP_BASE_URL} - a consumer fetching this
     * from outside (like goa_webapp_shop, running on a different domain) needs the real
     * public address, not whatever internal/reverse-proxy URL this request happened to
     * arrive on - and falls back to the current request's own URL.
     */
    private String resolveBaseUrl(HttpServletRequest request) {
        if (configuredBaseUrl != null) {
            return configuredBaseUrl;
        }
        return stripTrailingSlash(ServletUriComponentsBuilder.fromRequestUri(request)
                .replacePath(null)
                .build()
                .toUriString());
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
