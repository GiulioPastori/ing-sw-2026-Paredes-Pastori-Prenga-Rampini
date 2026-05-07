package it.polimi.it.view.gui.widgets;

import it.polimi.it.observer.OfferTileSnapshot;
import javafx.scene.image.Image;

import java.util.HashMap;
import java.util.Map;

/**
 * Caricatore (con cache) delle immagini delle tessere offerta.
 *
 * <p>Convenzione: ogni tessera ha un PNG in
 * {@code resources/it/polimi/it/view/gui/tiles/tile_<lettera>.png}
 * (es. {@code tile_A.png}, {@code tile_B.png}, …).
 * Se l'immagine non è presente viene restituito {@code null} e
 * {@link CardCellFactory} userà il rendering testuale già esistente.
 */
public final class TileImageCache {

    private static final String BASE_PATH = "/it/polimi/it/view/gui/tiles/";
    private static final Map<Character, Image> CACHE = new HashMap<>();

    private TileImageCache() { /* utility */ }

    /**
     * Restituisce l'immagine PNG associata alla tessera, oppure {@code null}
     * se non trovata fra le risorse.
     */
    public static Image imageFor(OfferTileSnapshot tile) {
        return imageFor(tile.letter());
    }

    public static Image imageFor(char letter) {
        Image cached = CACHE.get(letter);
        if (cached != null) return cached;

        String path = BASE_PATH + "tile_" + letter + ".png";
        try {
            var stream = TileImageCache.class.getResourceAsStream(path);
            if (stream == null) return null;
            Image img = new Image(stream);
            if (img.isError()) return null;
            CACHE.put(letter, img);
            return img;
        } catch (Exception ignored) {
            return null;
        }
    }
}