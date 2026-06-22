package it.polimi.it.observer;

import it.polimi.it.cards.Card;
import it.polimi.it.enums.TotemColour;
import it.polimi.it.tiles.OfferTile;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Immutable snapshot of an offer tile.
 *
 * <p>It holds the full lists of the cards shown in the two board rows
 * (events included: they can be seen but not taken), so the view can show
 * the player all available cards before deciding where to place the totem.
 *
 * <p>The cards are shared by all tiles: they are not assigned to one tile.
 * {@code topRowCards} and {@code bottomRowCards} say how many cards the
 * player CAN TAKE from that row if they choose this tile, not how many cards
 * are there.
 *
 * <p>{@link #placedTotem()} gives the colour of the totem now on the tile, or
 * {@code null} if the tile is free. The view uses it to draw the totem on the tile.
 *
 * @param letter tile letter
 * @param topRowCards number of top-row cards the tile lets the player take
 * @param bottomRowCards number of bottom-row cards the tile lets the player take
 * @param isFoodOnly true when the tile grants food instead of cards
 * @param isFree true when no totem is currently placed on the tile
 * @param placedTotem totem colour on the tile, or {@code null} if free
 * @param topCards current top-row card snapshots
 * @param bottomCards current bottom-row card snapshots
 */
public record OfferTileSnapshot(
        char               letter,
        int                topRowCards,
        int                bottomRowCards,
        boolean            isFoodOnly,
        boolean            isFree,
        TotemColour        placedTotem,
        List<CardSnapshot> topCards,
        List<CardSnapshot> bottomCards
) implements Serializable {
    @Serial private static final long serialVersionUID = 3L;

    /**
     * Creates a snapshot from a real tile, including all cards shown in the
     * two board rows and the totem on it, if any.
     *
     * @param tile the offer tile
     * @param topRow current top row of the board
     * @param bottomRow current bottom row of the board
     * @return snapshot of the tile and visible rows
     */
    public static OfferTileSnapshot of(OfferTile tile,
                                       List<Card> topRow,
                                       List<Card> bottomRow) {
        List<CardSnapshot> top    = topRow.stream().map(CardSnapshot::of).toList();
        List<CardSnapshot> bottom = bottomRow.stream().map(CardSnapshot::of).toList();
        return new OfferTileSnapshot(
                tile.getLetter(),
                tile.getTopRowCards(),
                tile.getBottomRowCards(),
                tile.isFoodOnly(),
                tile.isFree(),
                tile.getPlacedTotem(),
                top,
                bottom);
    }

    /**
     * Creates a snapshot without card lists, used where rows are not available.
     * The lists are empty, so the view shows only the counts.
     *
     * @param tile the offer tile
     * @return snapshot of the tile without visible row contents
     */
    public static OfferTileSnapshot of(OfferTile tile) {
        return new OfferTileSnapshot(
                tile.getLetter(),
                tile.getTopRowCards(),
                tile.getBottomRowCards(),
                tile.isFoodOnly(),
                tile.isFree(),
                tile.getPlacedTotem(),
                List.of(),
                List.of());
    }
}
