package it.polimi.it.view.gui.widgets;

import it.polimi.it.enums.TotemColour;
import javafx.scene.image.Image;

import java.util.EnumMap;
import java.util.Map;

/**
 * Cached loader for totem images.
 *
 * <p>Convention: each totem has a transparent-background PNG in
 * {@code resources/it/polimi/it/view/gui/cards/totem_<color>.png}, where
 * {@code <color>} is the lowercase enum name, for example
 * {@code totem_red.png}, {@code totem_white.png}).
 *
 * <p>The white totem remains visible on dark backgrounds because the source
 * PNG is light gray rather than pure white; no tinting logic is applied here.
 *
 * <p>If the image is missing, this class returns {@code null} and
 * {@link CardCellFactory} falls back to a coloured-dot placeholder.
 */
public final class TotemImageCache {

    private static final String BASE_PATH = "/it/polimi/it/view/gui/cards/";
    private static final Map<TotemColour, Image> CACHE = new EnumMap<>(TotemColour.class);
    /** Set true after we've already attempted to load each colour, so we
     *  don't keep retrying when an asset is genuinely missing. */
    private static final Map<TotemColour, Boolean> ATTEMPTED = new EnumMap<>(TotemColour.class);

    private TotemImageCache() { /* utility */ }

    /**
     * Returns the totem image for the given colour, or {@code null} if
     * the asset is missing.
     */
    public static Image imageFor(TotemColour colour) {
        if (colour == null) return null;
        Image cached = CACHE.get(colour);
        if (cached != null) return cached;
        if (Boolean.TRUE.equals(ATTEMPTED.get(colour))) return null;
        ATTEMPTED.put(colour, Boolean.TRUE);

        String path = BASE_PATH + "totem_" + colour.name().toLowerCase() + ".png";
        try {
            var stream = TotemImageCache.class.getResourceAsStream(path);
            if (stream == null) return null;
            Image img = new Image(stream);
            if (img.isError()) return null;
            CACHE.put(colour, img);
            return img;
        } catch (Exception ignored) {
            return null;
        }
    }
}
