package it.polimi.it.view.gui.widgets;

import it.polimi.it.enums.Era;
import it.polimi.it.observer.CardSnapshot;
import javafx.scene.image.Image;

import java.util.HashMap;
import java.util.Map;

/**
 * Cached loader for card images.
 *
 * Convention: each card has a PNG in
 * {@code resources/it/polimi/it/view/gui/cards/card_<id>.png}.
 * The generic card back is {@code cards/card_back.png}.
 * Era-specific backs are {@code cards/tribe_back_era1.png},
 * {@code tribe_back_era2.png}, {@code tribe_back_era3.png}.
 */
public final class CardImageCache {

    private static final String BASE_PATH = "/it/polimi/it/view/gui/cards/";
    private static final Map<Integer, Image> CACHE = new HashMap<>();

    /** Cached generic card back. */
    private static Image   cachedBack;
    private static boolean backLoaded;

    /** Per-era back cache (index: 0=ERA_I, 1=ERA_II, 2=ERA_III). */
    private static final Image[]   ERA_BACKS       = new Image[3];
    private static final boolean[] ERA_BACK_LOADED = new boolean[3];

    private static final String[] ERA_BACK_FILES = {
            "tribe_back_era1.png",
            "tribe_back_era2.png",
            "tribe_back_era3.png"
    };

    private CardImageCache() {}

    /** Returns the PNG for a card snapshot, or {@code null} if missing. */
    public static Image imageFor(CardSnapshot card) { return imageFor(card.id()); }

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
        } catch (Exception ignored) { return null; }
    }

    /**
     * Returns the card back image for the given era.
     * Falls back to the generic card_back.png if the era-specific file is missing.
     */
    public static Image cardBackForEra(Era era) {
        int idx = switch (era) {
            case ERA_I   -> 0;
            case ERA_II  -> 1;
            case ERA_III -> 2;
        };
        if (!ERA_BACK_LOADED[idx]) {
            ERA_BACK_LOADED[idx] = true;
            try {
                var stream = CardImageCache.class.getResourceAsStream(
                        BASE_PATH + ERA_BACK_FILES[idx]);
                if (stream != null) {
                    Image img = new Image(stream);
                    if (!img.isError()) ERA_BACKS[idx] = img;
                }
            } catch (Exception ignored) {}
        }
        return ERA_BACKS[idx] != null ? ERA_BACKS[idx] : cardBack();
    }

    /**
     * Returns the generic card back image, or {@code null} if missing.
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
        } catch (Exception ignored) {}
        return cachedBack;
    }
}
