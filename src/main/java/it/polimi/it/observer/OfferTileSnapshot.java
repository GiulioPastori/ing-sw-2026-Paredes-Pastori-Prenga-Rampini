package it.polimi.it.observer;

import it.polimi.it.tiles.OfferTile;

/**
 * Immutable snapshot of an offer tile.
 *
 * Completes the set of Value Objects (PlayerSnapshot, CardSnapshot)
 * carried by GameEvents. The View and Controller never receive a raw
 * OfferTile: they work exclusively with snapshots.
 *
 * The Controller resolves a chosen snapshot back to the concrete tile
 * via {@code game.resolveTile(snapshot)} before building a PlaceTotemAction.
 */
public record OfferTileSnapshot(
        char    letter,
        int     topRowCards,
        int     bottomRowCards,
        boolean isFoodOnly,
        boolean isFree
) {
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