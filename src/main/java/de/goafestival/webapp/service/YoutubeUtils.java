package de.goafestival.webapp.service;

import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Converts a regular YouTube URL (watch/share/short link) into an embeddable player URL. */
public final class YoutubeUtils {

    private static final Pattern ID_PATTERN = Pattern.compile(
            "(?:youtu\\.be/|youtube\\.com/(?:embed/|v/|watch\\?v=|watch\\?.*&v=|shorts/))([a-zA-Z0-9_-]{6,})");

    private YoutubeUtils() {
    }

    public static String toEmbedUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return null;
        }
        Matcher matcher = ID_PATTERN.matcher(url);
        if (matcher.find()) {
            return "https://www.youtube-nocookie.com/embed/" + matcher.group(1);
        }
        return url;
    }
}
