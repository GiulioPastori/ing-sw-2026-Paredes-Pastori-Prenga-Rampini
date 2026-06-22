package it.polimi.it.view.gui.widgets;

import it.polimi.it.observer.OfferTileSnapshot;
import javafx.scene.image.Image;

import java.util.HashMap;
import java.util.Map;

/**
 * Cached loader for offer tile images.
 *
 * <p>Convention: each tile has a PNG in
 * {@code resources/it/polimi/it/view/gui/tiles/tile_<letter>.png}
 * (for example {@code tile_A.png}, {@code tile_B.png}). Available letters are
 * A-G, enough for the six offer tiles plus the food-only tile in a 5-player
 * game. If the image is missing, this class returns {@code null} and
 * {@link CardCellFactory} uses a textual placeholder.
 */
public final class TileImageCache {

    private static final String BASE_PATH = "/it/polimi/it/view/gui/tiles/";
    private static final Map<Character, Image> CACHE = new HashMap<>();

    private TileImageCache() { /* utility */ }

    /**
     * Returns the image for an offer tile based on its letter, or
     * {@code null} if the asset is missing.
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

    /**
     * Returns the variant image for the Turn Order Tile based on player count.
     *
     * <p>Convention: {@code tiles/<numPlayers>_players_tile.png}, for example
     * {@code 2_players_tile.png}). Returns {@code null} if the asset is not
     * present (the {@link CardCellFactory} falls back to a procedural,
     * stylesheet-only rendering).
     */
    public static Image turnOrderTileFor(int numPlayers) {
        String path = BASE_PATH + numPlayers + "_players_tile.png";
        try {
            var stream = TileImageCache.class.getResourceAsStream(path);
            if (stream == null) return null;
            Image img = new Image(stream);
            if (img.isError()) return null;
            return img;
        } catch (Exception ignored) {
            return null;
        }
    }
}
