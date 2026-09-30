package de.goafestival.webapp.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Optional whole-site HTTP Basic gate for a non-production ("test.*") deployment,
 * enabled via TEST_MODE_ENABLED. Runs before Spring Security's own filters, so it
 * applies to literally every request - including /admin and /login - completely
 * independent of the admin login underneath it. Off (no-op) unless enabled.
 */
public class SiteLockFilter extends OncePerRequestFilter {

    private final String username;
    private final String password;

    public SiteLockFilter(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
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
