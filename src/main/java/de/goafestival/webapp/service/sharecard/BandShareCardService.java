package de.goafestival.webapp.service.sharecard;

import de.goafestival.webapp.domain.Band;
import de.goafestival.webapp.domain.Edition;
import de.goafestival.webapp.domain.Location;
import de.goafestival.webapp.repository.BandRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Renders a shareable "trading card" style PNG for one band - built to be posted to an
 * Instagram story/feed, so it reuses that year's existing branding (background image,
 * logo, colors) rather than needing any extra artwork per band. Everything is drawn with
 * Java2D directly onto a 1080x1350 canvas (Instagram's portrait post size).
 */
@Service
public class BandShareCardService {

    private static final Logger log = LoggerFactory.getLogger(BandShareCardService.class);

    private static final int WIDTH = 1080;
    private static final int HEIGHT = 1350;

    // Bumped whenever render(Band) visually changes - folded into the cache filename so a
    // code deploy invalidates every cached card on its own. Without this, redeploying a
    // design tweak would keep serving pre-existing PNGs from disk indefinitely, since the
    // on-disk cache otherwise only reacts to band/edition *data* changes, not code changes.
    private static final int RENDER_VERSION = 4;

    private final BandRepository bandRepository;
    private final Path uploadRoot;
    private final Path cacheDir;
    private Font displayFont;

    public BandShareCardService(BandRepository bandRepository, @Value("${app.upload-dir:uploads}") String uploadDir) {
        this.bandRepository = bandRepository;
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
        this.cacheDir = uploadRoot.resolve("sharecards");
    }

    @PostConstruct
    void loadFont() {
        // Reuses the site's own display font (see --font-display in style.css) so the
        // card looks like it belongs to the site instead of introducing a second typeface.
        try (InputStream in = getClass().getResourceAsStream("/static/fonts/Brugty.ttf")) {
            displayFont = Font.createFont(Font.TRUETYPE_FONT, in);
        } catch (IOException | FontFormatException e) {
            log.warn("Konnte die Schriftart für die Share-Karte nicht laden, falle auf eine Systemschrift zurück.", e);
            displayFont = new Font(Font.SANS_SERIF, Font.BOLD, 12);
        }
        try {
            Files.createDirectories(cacheDir);
        } catch (IOException e) {
            log.warn("Konnte das Cache-Verzeichnis für Share-Karten nicht anlegen.", e);
        }
    }

    /**
     * Like {@link #render(Band)}, but reads/writes a copy on disk under the upload
     * directory - the Hall-of-Fame page shows dozens of these at once, so re-rendering
     * each one with Java2D on every request would be wasteful. The cache is invalidated
     * (see {@link #invalidate(Long)}/{@link #invalidateForEdition(Long)}) whenever the
     * band or its edition's branding changes, so it's safe to keep indefinitely otherwise.
     */
    public byte[] renderCached(Band band) throws IOException {
        Path cached = cacheDir.resolve(cacheFilename(band.getId()));
        if (Files.exists(cached)) {
            return Files.readAllBytes(cached);
        }
        byte[] png = render(band);
        Files.write(cached, png);
        return png;
    }

    /** Drops the cached share card for one band, e.g. after it was edited. */
    public void invalidate(Long bandId) {
        try {
            Files.deleteIfExists(cacheDir.resolve(cacheFilename(bandId)));
        } catch (IOException e) {
            log.warn("Konnte den Share-Karten-Cache für Band {} nicht löschen.", bandId, e);
        }
    }

    private String cacheFilename(Long bandId) {
        return bandId + "-v" + RENDER_VERSION + ".png";
    }

    /**
     * Drops every cached share card for an edition's bands - needed not just when the
     * edition's own branding changes (colors/logo/background, which every card in it
     * reflects), but also whenever one band is added, edited or removed, since each
     * card's "No. XX" slot number depends on where it falls among its siblings.
     */
    public void invalidateForEdition(Long editionId) {
        bandRepository.findByEditionIdOrderByPerformanceAtAsc(editionId)
                .forEach(band -> invalidate(band.getId()));
    }

    /** 1-based position of this band within its edition's whole running order (day-spanning). */
    public int slotNumber(Band band) {
        List<Band> ordered = bandRepository.findByEditionIdOrderByPerformanceAtAsc(band.getEdition().getId());
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).getId().equals(band.getId())) {
                return i + 1;
            }
        }
        return 1;
    }

    public byte[] render(Band band) throws IOException {
        Edition edition = band.getEdition();
        int slot = slotNumber(band);

        BufferedImage canvas = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // The card's background panel uses the real Primärfarbe (not yet used anywhere
            // else on the site) - falls back to the Sekundärfarbe for an edition that hasn't
            // had one set yet, so older/archived editions still render a sensible card.
            String primaryHex = StringUtils.hasText(edition.getColorPrimary()) ? edition.getColorPrimary() : edition.getColorSecondary();
            Color primary = parseColor(primaryHex, new Color(0x4a1f2b));
            Color accent = parseColor(edition.getColorAccent(), new Color(0xf2c14e));
            Color accent2 = parseColor(edition.getColorAccent2(), new Color(0xc0392b));
            Color secondaryColor = parseColor(edition.getColorSecondary(), new Color(0x2f6f68));

            paintBorderBackground(g, edition, primary);

            int outerMargin = 32;
            int cardX = outerMargin, cardY = outerMargin;
            int cardW = WIDTH - 2 * outerMargin, cardH = HEIGHT - 2 * outerMargin;
            int cardRadius = 56;

            paintPanel(g, cardX, cardY, cardW, cardH, cardRadius, primary);
            // A layered "frame within a frame" instead of one flat stroke - a bright
            // hairline right at the edge, the structural dark stroke just inside it, then
            // a thin accent lip closest to the content - for the trading-card look.
            paintPanelBorder(g, cardX, cardY, cardW, cardH, cardRadius, primary.brighter(), 3f);
            paintPanelBorder(g, cardX, cardY, cardW, cardH, cardRadius, primary.darker(), 7f, 5f);
            paintPanelBorder(g, cardX, cardY, cardW, cardH, cardRadius, accent, 3f, 16f);

            int pad = 40;
            int contentX = cardX + pad;
            int contentW = cardW - 2 * pad;
            int rightEdge = cardX + cardW - pad;

            // --- header: band name + slot number ---
            int headerY = cardY + pad;
            Font nameFont = fitFont(g, band.getName().toUpperCase(Locale.GERMAN), displayFont, 112, 52, contentW - 170);
            g.setFont(nameFont);
            g.setColor(accent);
            FontMetrics nameMetrics = g.getFontMetrics();
            int nameBaseline = headerY + nameMetrics.getAscent();
            g.drawString(band.getName().toUpperCase(Locale.GERMAN), contentX, nameBaseline);

            String slotLabel = String.format("No. %02d", slot);
            Font slotFont = displayFont.deriveFont(50f);
            g.setFont(slotFont);
            g.setColor(accent2);
            FontMetrics slotMetrics = g.getFontMetrics();
            g.drawString(slotLabel, rightEdge - slotMetrics.stringWidth(slotLabel), headerY + slotMetrics.getAscent());

            int headerHeight = Math.max(nameMetrics.getAscent() + nameMetrics.getDescent(), slotMetrics.getAscent() + slotMetrics.getDescent());

            // --- band photo ---
            int photoY = headerY + headerHeight + 28;
            int photoH = 600;
            // Same radius as the outer card panel - a tighter curve here would read as two
            // competing roundings instead of one consistent "trading card" shape.
            int photoRadius = cardRadius;
            BufferedImage photo = loadImage(band.getMainImagePath());
            paintPanel(g, contentX, photoY, contentW, photoH, photoRadius, Color.BLACK);
            withClip(g, contentX, photoY, contentW, photoH, photoRadius, clipped -> {
                if (photo != null) {
                    drawCover(clipped, photo, contentX, photoY, contentW, photoH);
                } else {
                    paintPlaceholderGradient(clipped, contentX, photoY, contentW, photoH, primary, accent2);
                }
            });
            // The photo gets its own "art window" frame, like a trading card's inset
            // artwork border - distinct from the outer card frame around it. A real
            // trading card's art window also stays free of text/stats, which all live in
            // their own boxes below it (see the info panels further down).
            paintPanelBorder(g, contentX, photoY, contentW, photoH, photoRadius, accent, 5f);

            // --- edition logo badge, overlapping the photo's top-right corner like a tilted sticker ---
            // No backing shape or ring - just the logo artwork itself, which usually already
            // brings its own shape/border.
            BufferedImage logo = loadImage(edition.getLogoImagePath());
            if (logo != null) {
                int badgeSize = 250;
                int badgeCx = contentX + contentW - 110;
                int badgeCy = photoY + 55;
                AffineTransform oldTransform = g.getTransform();
                g.rotate(Math.toRadians(14), badgeCx, badgeCy);
                double logoScale = Math.min((double) badgeSize / logo.getWidth(), (double) badgeSize / logo.getHeight());
                int lw = (int) Math.round(logo.getWidth() * logoScale);
                int lh = (int) Math.round(logo.getHeight() * logoScale);
                g.drawImage(logo, badgeCx - lw / 2, badgeCy - lh / 2, lw, lh, null);
                g.setTransform(oldTransform);
            }

            // --- info panels ---
            int panelGap = 22;
            int panelY = photoY + photoH + 26;
            int panelRadius = 24;
            // Info panels use the Sekundärfarbe rather than a shade of the card's own primary
            // panel, so they read as a clearly separate surface instead of blending into it -
            // accent-colored text on top keeps the same light-on-dark contrast as the header.
            Color panelBg = withAlpha(secondaryColor, 242);
            Color textColor = accent;

            boolean hasGenre = StringUtils.hasText(band.getGenre());
            boolean hasHerkunft = StringUtils.hasText(band.getHerkunft());
            if (hasGenre || hasHerkunft) {
                int panelH = 152;
                paintPanel(g, contentX, panelY, contentW, panelH, panelRadius, panelBg);
                paintLayeredPanelBorder(g, contentX, panelY, contentW, panelH, panelRadius, secondaryColor.brighter(), accent);
                int half = contentW / 2;
                if (hasGenre) {
                    drawIconLabelValue(g, iconMusicNote(), contentX + 32, panelY, half - 32, panelH, "Genre", band.getGenre(), textColor);
                }
                if (hasHerkunft) {
                    drawIconLabelValue(g, iconPin(), contentX + half + 32, panelY, half - 64, panelH, "Herkunft", band.getHerkunft(), textColor);
                }
                if (hasGenre && hasHerkunft) {
                    g.setColor(new Color(0, 0, 0, 40));
                    g.fillRect(contentX + half, panelY + 24, 2, panelH - 48);
                }
                panelY += panelH + panelGap;
            }

            if (band.getPerformanceAt() != null) {
                int panelH = 118;
                paintPanel(g, contentX, panelY, contentW, panelH, panelRadius, panelBg);
                paintLayeredPanelBorder(g, contentX, panelY, contentW, panelH, panelRadius, secondaryColor.brighter(), accent);
                String label = formatPerformanceLabel(band);
                drawCenteredIconText(g, iconCalendar(), contentX, panelY, contentW, panelH, label, textColor);
                panelY += panelH + panelGap;
            }

            Location location = edition.getLocation();
            if (location != null && StringUtils.hasText(location.getName())) {
                int panelH = 118;
                paintPanel(g, contentX, panelY, contentW, panelH, panelRadius, panelBg);
                paintLayeredPanelBorder(g, contentX, panelY, contentW, panelH, panelRadius, secondaryColor.brighter(), accent);
                String label = StringUtils.hasText(location.getZipCity())
                        ? location.getName() + ", " + location.getZipCity()
                        : location.getName();
                drawCenteredIconText(g, iconPin(), contentX, panelY, contentW, panelH, label, textColor);
            }
        } finally {
            g.dispose();
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(canvas, "png", out);
        return out.toByteArray();
    }

    private String formatPerformanceLabel(Band band) {
        DateTimeFormatter dayMonth = DateTimeFormatter.ofPattern("EEEE d. MMMM", Locale.GERMAN);
        DateTimeFormatter time = DateTimeFormatter.ofPattern("HH:mm", Locale.GERMAN);
        return dayMonth.format(band.getPerformanceAt()) + " " + time.format(band.getPerformanceAt()) + " Uhr";
    }

    // ---------------------------------------------------------------- drawing helpers

    private void paintBorderBackground(Graphics2D g, Edition edition, Color fallback) {
        BufferedImage background = loadImage(edition.getBackgroundImagePath());
        if (background != null) {
            drawCover(g, background, 0, 0, WIDTH, HEIGHT);
        } else {
            g.setColor(fallback.darker());
            g.fillRect(0, 0, WIDTH, HEIGHT);
        }
    }

    private void paintPanel(Graphics2D g, int x, int y, int w, int h, int radius, Color color) {
        g.setColor(color);
        g.fill(new RoundRectangle2D.Float(x, y, w, h, radius, radius));
    }

    private void paintPanelBorder(Graphics2D g, int x, int y, int w, int h, int radius, Color color) {
        paintPanelBorder(g, x, y, w, h, radius, color, 4f);
    }

    private void paintPanelBorder(Graphics2D g, int x, int y, int w, int h, int radius, Color color, float strokeWidth) {
        paintPanelBorder(g, x, y, w, h, radius, color, strokeWidth, 0f);
    }

    /**
     * Like the other overload, but lets several calls at increasing {@code extraInset}
     * draw concentric rings instead of a single flat stroke - the layered "frame within a
     * frame" look trading cards (Pokémon, Yu-Gi-Oh) use instead of a plain border.
     */
    private void paintPanelBorder(Graphics2D g, int x, int y, int w, int h, int radius, Color color, float strokeWidth, float extraInset) {
        g.setColor(color);
        g.setStroke(new BasicStroke(strokeWidth));
        float inset = extraInset + strokeWidth / 2f;
        float r = Math.max(0, radius - extraInset);
        g.draw(new RoundRectangle2D.Float(x + inset, y + inset, w - 2 * inset, h - 2 * inset, r, r));
    }

    /** A scaled-down echo of the outer card's layered frame, for the smaller info panels. */
    private void paintLayeredPanelBorder(Graphics2D g, int x, int y, int w, int h, int radius, Color highlightColor, Color accentColor) {
        paintPanelBorder(g, x, y, w, h, radius, highlightColor, 2f);
        paintPanelBorder(g, x, y, w, h, radius, accentColor, 3f, 5f);
    }

    private void paintPlaceholderGradient(Graphics2D g, int x, int y, int w, int h, Color from, Color to) {
        Paint previous = g.getPaint();
        g.setPaint(new GradientPaint(x, y, from, x + w, y + h, to));
        g.fillRect(x, y, w, h);
        g.setPaint(previous);
    }

    private interface ClippedDraw {
        void draw(Graphics2D g);
    }

    private void withClip(Graphics2D g, int x, int y, int w, int h, int radius, ClippedDraw draw) {
        Shape oldClip = g.getClip();
        g.setClip(new RoundRectangle2D.Float(x, y, w, h, radius, radius));
        draw.draw(g);
        g.setClip(oldClip);
    }

    /** Scales+crops an image to fill the given rect, like CSS background-size: cover. */
    private void drawCover(Graphics2D g, BufferedImage img, int x, int y, int w, int h) {
        double scale = Math.max((double) w / img.getWidth(), (double) h / img.getHeight());
        int sw = (int) Math.ceil(img.getWidth() * scale);
        int sh = (int) Math.ceil(img.getHeight() * scale);
        int sx = x - (sw - w) / 2;
        int sy = y - (sh - h) / 2;
        g.drawImage(img, sx, sy, sw, sh, null);
    }

    /** Maps the image's luminance onto a shadow-to-highlight color ramp (a vintage photo-poster look). */
    private BufferedImage duotone(BufferedImage src, Color shadow, Color highlight) {
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int[] row = new int[w];
        int[] outRow = new int[w];
        for (int y = 0; y < h; y++) {
            src.getRGB(0, y, w, 1, row, 0, w);
            for (int x = 0; x < w; x++) {
                int argb = row[x];
                int a = (argb >>> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF, gC = (argb >> 8) & 0xFF, b = argb & 0xFF;
                double lum = (0.299 * r + 0.587 * gC + 0.114 * b) / 255.0;
                int nr = clamp((int) Math.round(shadow.getRed() + lum * (highlight.getRed() - shadow.getRed())));
                int ng = clamp((int) Math.round(shadow.getGreen() + lum * (highlight.getGreen() - shadow.getGreen())));
                int nb = clamp((int) Math.round(shadow.getBlue() + lum * (highlight.getBlue() - shadow.getBlue())));
                outRow[x] = (a << 24) | (nr << 16) | (ng << 8) | nb;
            }
            out.setRGB(0, y, w, 1, outRow, 0, w);
        }
        return out;
    }

    private int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    private Color parseColor(String hex, Color fallback) {
        if (!StringUtils.hasText(hex)) {
            return fallback;
        }
        try {
            return Color.decode(hex);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /**
     * Picks the largest size (down to a minimum) at which the text fits within maxWidth -
     * so a short value (e.g. "Hamburg") renders much bigger than a long one (e.g.
     * "Psychedelic Noise Punk") instead of both settling for one size picked to fit the
     * longest case.
     */
    private Font fitFont(Graphics2D g, String text, Font base, float maxSize, float minSize, int maxWidth) {
        float size = maxSize;
        while (size > minSize) {
            Font candidate = base.deriveFont(size);
            FontRenderContext frc = g.getFontRenderContext();
            double width = new TextLayout(text, candidate, frc).getBounds().getWidth();
            if (width <= maxWidth) {
                return candidate;
            }
            size -= 2;
        }
        return base.deriveFont(minSize);
    }

    private BufferedImage loadImage(String publicPath) {
        if (!StringUtils.hasText(publicPath) || !publicPath.startsWith("/uploads/")) {
            return null;
        }
        Path file = uploadRoot.resolve(publicPath.substring("/uploads/".length())).normalize();
        if (!file.startsWith(uploadRoot) || !Files.exists(file)) {
            return null;
        }
        try (InputStream in = Files.newInputStream(file)) {
            return ImageIO.read(in);
        } catch (IOException e) {
            log.warn("Konnte Bild für die Share-Karte nicht laden: {}", file, e);
            return null;
        }
    }

    // ---------------------------------------------------------------- icons (hand-drawn, no asset files)

    /** Fixed size for the Genre/Herkunft/Termin/Location field icons - stays constant regardless of the value text's own (dynamic) size. */
    private static final int FIELD_ICON_SIZE = 48;

    private void drawIconLabelValue(Graphics2D g, Path2D icon, int x, int y, int w, int h, String label, String value, Color color) {
        int iconSize = FIELD_ICON_SIZE;
        int iconX = x;
        int iconY = y + h / 2 - iconSize / 2;
        g.setColor(color);
        drawIcon(g, icon, iconX, iconY, iconSize);

        int textX = iconX + iconSize + 18;
        Font labelFont = new Font(Font.SANS_SERIF, Font.PLAIN, 27);
        g.setFont(labelFont);
        FontMetrics lm = g.getFontMetrics();
        int centerY = y + h / 2;
        g.setColor(withAlpha(color, 170));
        g.drawString(label, textX, centerY - 10);

        // Capped by both the column width (long values shrink) and what's left of the
        // panel's height below the label (very short values don't grow tall enough to
        // collide with it).
        int availableWidth = Math.max(40, x + w - textX);
        int availableHeight = Math.max(24, y + h - (centerY - 10 + lm.getDescent()) - 14);
        float maxValueSize = Math.min(72, availableHeight);
        g.setFont(fitFont(g, value, displayFont, maxValueSize, 24, availableWidth));
        FontMetrics vm = g.getFontMetrics();
        g.setColor(color);
        g.drawString(value, textX, centerY - 10 + lm.getDescent() + vm.getAscent());
    }

    private void drawCenteredIconText(Graphics2D g, Path2D icon, int x, int y, int w, int h, String text, Color color) {
        float maxSize = Math.min(72, h - 36);
        Font font = fitFont(g, text, displayFont, maxSize, 26, w - 170);
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        int iconSize = FIELD_ICON_SIZE;
        int textWidth = fm.stringWidth(text);
        int totalWidth = iconSize + 16 + textWidth;
        int startX = x + (w - totalWidth) / 2;
        g.setColor(color);
        drawIcon(g, icon, startX, y + h / 2 - iconSize / 2, iconSize);
        g.drawString(text, startX + iconSize + 16, y + h / 2 - (fm.getAscent() + fm.getDescent()) / 2 + fm.getAscent());
    }

    private void drawIcon(Graphics2D g, Path2D template, int x, int y, int size) {
        AffineTransform t = AffineTransform.getTranslateInstance(x, y);
        t.scale(size / 24.0, size / 24.0);
        g.fill(t.createTransformedShape(template));
    }

    /** A simple eighth-note glyph, drawn on a 24x24 grid (plain union of positive shapes). */
    private Path2D iconMusicNote() {
        Path2D path = new Path2D.Float();
        path.append(new Ellipse2D.Float(2, 15, 7.5f, 6), false);
        path.append(new Rectangle2D.Float(8.3f, 2, 2.2f, 16.5f), false);
        Path2D flag = new Path2D.Float();
        flag.moveTo(10.5, 2);
        flag.curveTo(17, 3.5, 17, 9.5, 10.5, 11.5);
        flag.lineTo(10.5, 8);
        flag.curveTo(14.5, 6.8, 14.5, 4.2, 10.5, 2);
        flag.closePath();
        path.append(flag, false);
        return path;
    }

    /** A simple map-pin glyph, drawn on a 24x24 grid: outline with a circular hole punched via even-odd winding. */
    private Path2D iconPin() {
        Path2D outline = new Path2D.Float();
        outline.moveTo(12, 23);
        outline.curveTo(12, 23, 4, 14.5, 4, 9);
        outline.curveTo(4, 4.6, 7.6, 1, 12, 1);
        outline.curveTo(16.4, 1, 20, 4.6, 20, 9);
        outline.curveTo(20, 14.5, 12, 23, 12, 23);
        outline.closePath();
        Path2D result = new Path2D.Float(Path2D.WIND_EVEN_ODD);
        result.append(outline, false);
        result.append(new Ellipse2D.Float(8.2f, 5.2f, 7.6f, 7.6f), false);
        return result;
    }

    /** A simple calendar glyph, drawn on a 24x24 grid: body plus two top tabs (plain union, no holes). */
    private Path2D iconCalendar() {
        Path2D path = new Path2D.Float();
        path.append(new RoundRectangle2D.Float(1.5f, 4, 21, 18, 4, 4), false);
        path.append(new RoundRectangle2D.Float(4.3f, 0.5f, 2.6f, 6.5f, 1.3f, 1.3f), false);
        path.append(new RoundRectangle2D.Float(17.1f, 0.5f, 2.6f, 6.5f, 1.3f, 1.3f), false);
        return path;
    }
}
