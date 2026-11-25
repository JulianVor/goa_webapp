package de.goafestival.webapp.web.api;

/**
 * The subset of the current edition's/site's branding that other apps (currently just
 * goa_webapp_shop) need to render in the same look, without them having direct database
 * access to this app. Mirrors exactly the CSS custom properties fragments/head.html
 * injects for the public pages - a consumer can drop these straight into its own
 * ":root { ... }" block.
 */
public record BrandingResponse(
        String siteTitle,
        String logoImageUrl,
        String backgroundImageUrl,
        String colorPrimary,
        String colorSecondary,
        String colorAccent,
        String colorBox,
        String colorAccentBox,
        String colorHeading,
        String colorButton,
        String colorHeadingOutline,
        String colorText,
        String boxRadius,
        String pillRadius,
        String fontDisplay,
        String fontBody,
        String fontWeightDisplay,
        String fontWeightBody) {
}
