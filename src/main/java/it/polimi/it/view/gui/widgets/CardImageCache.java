package it.polimi.it.view.gui.widgets;

import it.polimi.it.observer.CardSnapshot;
import javafx.scene.image.Image;

import java.util.HashMap;
import java.util.Map;

/**
 * Caricatore (con cache) delle immagini delle carte.
 *
 * <p>Convenzione: ogni carta ha un PNG in
 * {@code resources/it/polimi/it/view/gui/cards/<id>.png} (es.
 * {@code cards/1001.png}). Se l'immagine non è presente, viene
 * restituito {@code null} e la {@link CardCellFactory} userà un placeholder
 * testuale.
 *
 * <p>L'utente del progetto si è impegnato a fornire le immagini: questa classe
 * non fallisce se mancano, così la GUI rimane utilizzabile durante lo sviluppo.
 */
public final class CardImageCache {

    private static final String BASE_PATH = "/it/polimi/it/view/gui/cards/";
    private static final Map<Integer, Image> CACHE = new HashMap<>();
    private static final Object MISSING = new Object(); // sentinella

    private CardImageCache() { /* utility */ }

    /**
     * Restituisce l'immagine PNG associata alla carta, oppure {@code null} se
     * non trovata fra le risorse.
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
}
