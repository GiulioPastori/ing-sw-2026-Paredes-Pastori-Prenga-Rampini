package it.polimi.it.view.gui.widgets;

import it.polimi.it.enums.TotemColour;
import javafx.scene.image.Image;

import java.util.EnumMap;
import java.util.Map;

/**
 * Caricatore (con cache) delle immagini dei totem.
 *
 * <p>Convenzione: ogni totem ha un PNG con sfondo trasparente in
 * {@code resources/it/polimi/it/view/gui/cards/totem_<color>.png}, dove
 * {@code <color>} è la versione minuscola del nome dell'enum (es.
 * {@code totem_red.png}, {@code totem_white.png}).
 *
 * <p>Il totem bianco rimane visibile su sfondi scuri (è in realtà un grigio
 * chiaro, non bianco puro): questa è una proprietà del PNG di partenza, qui
 * non c'è logica di tinta.
 *
 * <p>Se l'immagine non è presente, viene restituito {@code null} e la
 * {@link CardCellFactory} ricade sul placeholder a pallino colorato.
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
