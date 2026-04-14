package it.polimi.it.observer;

import it.polimi.it.tiles.OfferTile;

import java.io.Serial;
import java.io.Serializable;

/**
 * Immutable snapshot of an offer tile.
 *
 * Implements Serializable so it can be passed inside Message objects
 * over RMI and via Java object serialisation over sockets.
 */
public record OfferTileSnapshot(
        char    letter,
        int     topRowCards,
        int     bottomRowCards,
        boolean isFoodOnly,
        boolean isFree
) implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    /** Creates a snapshot from the concrete tile. */
    public static OfferTileSnapshot of(OfferTile tile) {
        return new OfferTileSnapshot(
                tile.getLetter(),
                tile.getTopRowCards(),
                tile.getBottomRowCards(),
                tile.isFoodOnly(),
                tile.isFree()
        );
    }
}