package it.polimi.it.observer;

import it.polimi.it.cards.Card;
import it.polimi.it.enums.TotemColour;
import it.polimi.it.tiles.OfferTile;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

 /**
 * Immutable snapshot of an offer tile.
  * It holds the FULL lists of the cards shown in the two board rows
 * (events included: they can be seen but not taken), so the view can show
 * the player ALL the available cards before deciding where to place the totem.
 *
 * The cards are shared by all tiles — they are not "assigned" to one
 * tile. {topRowCards} and {bottomRowCards} say how many cards the
 * player CAN TAKE from that row if they choose this tile, not how many cards
 * are there.
 *
 * { #placedTotem()} gives the colour of the totem now on the tile, or
 * { null} if the tile is free. The view uses it to draw the totem on the tile
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

     /** Creates a snapshot from a real tile, including ALL the cards shown in the
      * two board rows (events included) and the totem on it, if any.
      *
      * tile=the offer tile
      * topRow=the current top row of the board
      * bottomRow=the current bottom row of the board
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

     /** Version without card lists (used where the rows are not available).
      * The lists will be empty — the view shows only the counts. */
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