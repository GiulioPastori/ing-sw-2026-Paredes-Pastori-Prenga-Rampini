package it.polimi.it.view.gui.widgets;

import it.polimi.it.observer.CardSnapshot;
import javafx.scene.image.Image;

import java.util.HashMap;
import java.util.Map;

/**
 * Caricatore (con cache) delle immagini delle carte.
 *
 * <p>Convenzione: ogni carta ha un PNG in
 * {@code resources/it/polimi/it/view/gui/cards/card_<id>.png} (es.
 * {@code cards/card_1001.png}). Il retro carta è in {@code cards/card_back.png}
 * e viene usato dal mazzo Tribù. Se l'immagine non è presente, viene
 * restituito {@code null} e la {@link CardCellFactory} userà un placeholder
 * testuale.
 */
public final class CardImageCache {

    private static final String BASE_PATH = "/it/polimi/it/view/gui/cards/";
    private static final Map<Integer, Image> CACHE = new HashMap<>();

    /** Cached card back; {@code null} until the first {@link #cardBack()} call. */
    private static Image cachedBack;
    /** True once we've attempted to load the back at least once. */
    private static boolean backLoaded;

    private CardImageCache() { /* utility */ }

    /**
     * Returns the PNG for a card snapshot, or {@code null} if missing.
     */
    public static Image imageFor(CardSnapshot card) {
        return imageFor(card.id());
    }

    public static Image imageFor(int cardId) {
        Image cached = CACHE.get(cardId);
        if (cached != null) return cached;

        String path = BASE_PATH + "card_" + cardId + ".png";
        try {
            var stream = CardImageCache.class.getResourceAsStream(path);
            if (stream == null) return null;
            Image img = new Image(stream);
            if (img.isError()) return null;
            CACHE.put(cardId, img);
            return img;
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Returns the card back image used to render the tribe deck, or
     * {@code null} if the asset is not available.
     */
    public static Image cardBack() {
        if (backLoaded) return cachedBack;
        backLoaded = true;
        try {
            var stream = CardImageCache.class.getResourceAsStream(BASE_PATH + "card_back.png");
            if (stream == null) return null;
            Image img = new Image(stream);
            if (img.isError()) return null;
            cachedBack = img;
        } catch (Exception ignored) {
            // cachedBack stays null
        }
        return cachedBack;
    }
}