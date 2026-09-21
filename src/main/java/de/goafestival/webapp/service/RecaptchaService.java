package de.goafestival.webapp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Verifies a Google reCAPTCHA v2 ("I'm not a robot") response server-side. */
@Service
public class RecaptchaService {

    private static final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    private final RestClient restClient;
    private final String siteKey;
    private final String secretKey;

    public RecaptchaService(@Value("${app.recaptcha.site-key:}") String siteKey,
                             @Value("${app.recaptcha.secret-key:}") String secretKey) {
        this.restClient = RestClient.create();
        this.siteKey = siteKey;
        this.secretKey = secretKey;
    }

    public boolean isConfigured() {
        return !siteKey.isBlank() && !secretKey.isBlank();
    }

    /** The public site key for rendering the widget, or an empty string if reCAPTCHA isn't configured. */
    public String getSiteKey() {
        return siteKey;
    }

    /** True when reCAPTCHA isn't configured (nothing to check) or the token verifies with Google. */
    public boolean verify(String token, String remoteIp) {
        if (!isConfigured()) {
            return true;
        }
        if (token == null || token.isBlank()) {
            return false;
        }
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("secret", secretKey);
        body.add("response", token);
        if (remoteIp != null) {
            body.add("remoteip", remoteIp);
        }
        try {
            VerifyResponse response = restClient.post()
                    .uri(VERIFY_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(VerifyResponse.class);
            return response != null && response.success();
        } catch (RestClientException e) {
            return false;
        }
    }

    private record VerifyResponse(boolean success) {
    }
}
