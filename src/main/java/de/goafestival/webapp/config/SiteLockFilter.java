package de.goafestival.webapp.config;

import de.goafestival.webapp.service.SiteSettingsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Optional whole-site HTTP Basic gate, turned on/off from the admin panel
 * (SiteSettings.testAccessEnabled) rather than at deploy time. Runs before Spring
 * Security's own filters, so it applies to literally every request - including
 * /admin and /login - completely independent of the admin login underneath it,
 * even though it reuses that same login's credentials. No-op while disabled.
 */
public class SiteLockFilter extends OncePerRequestFilter {

    private final String username;
    private final String password;
    private final SiteSettingsService siteSettingsService;

    public SiteLockFilter(String username, String password, SiteSettingsService siteSettingsService) {
        this.username = username;
        this.password = password;
        this.siteSettingsService = siteSettingsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // goa_webapp_shop fetches /api/branding at startup/periodically regardless of
        // whether this site is in test-access mode - it carries no private content (just
        // colors/fonts/metadata), and the shop would otherwise be stuck rendering with
        // stale/fallback branding for as long as the lock is on.
        if (!siteSettingsService.get().isTestAccessEnabled() || request.getRequestURI().startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Basic ")) {
            String decoded = new String(Base64.getDecoder().decode(header.substring(6)), StandardCharsets.UTF_8);
            int colon = decoded.indexOf(':');
            if (colon >= 0 && decoded.substring(0, colon).equals(username) && decoded.substring(colon + 1).equals(password)) {
                filterChain.doFilter(request, response);
                return;
            }
        }
        response.setHeader("WWW-Authenticate", "Basic realm=\"Testbetrieb\"");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }
}
