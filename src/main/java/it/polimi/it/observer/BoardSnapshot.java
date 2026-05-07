package it.polimi.it.observer;

import it.polimi.it.Board;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Immutable snapshot of the visible board state at a given moment.
 *
 * <p>Bundles everything the view needs to render the board:
 * <ul>
 *   <li>{@link #offerTiles} — all offer tiles in track order, each carrying
 *       its current top/bottom row cards. Free vs occupied is on the snapshot.</li>
 *   <li>{@link #turnOrderTile} — current Turn Order Tile state (variant by
 *       player count + which spaces are occupied by which totem).</li>
 *   <li>{@link #tribeDeckSize} — number of cards remaining in the tribe deck,
 *       so the view can render the deck pile.</li>
 * </ul>
 *
 * <p>Carried by events that change the board (e.g. {@code RoundStarted},
 * {@code TotemPlaced}). Adding a new field to this record affects only events
 * that already include a {@code BoardSnapshot}; consumers that ignore the
 * field continue to work.
 *
 * <p>Implements {@link Serializable} so it can be passed inside Message
 * objects over RMI and via Java object serialisation over sockets.
 */
public record BoardSnapshot(
        List<OfferTileSnapshot> offerTiles,
        TurnOrderTileSnapshot   turnOrderTile,
        int                     tribeDeckSize
) implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    /**
     * Builds a snapshot from the live {@link Board}. All offer tiles are
     * included (free and occupied); the offer-tile snapshots carry the
     * current top/bottom row cards.
     */
    public static BoardSnapshot of(Board board) {
        List<OfferTileSnapshot> tiles = board.getOfferTiles().getAllTiles().stream()
                .map(t -> OfferTileSnapshot.of(t, board.getTopRow(), board.getBottomRow()))
                .toList();
        return new BoardSnapshot(
                tiles,
                TurnOrderTileSnapshot.of(board.getTurnOrderTile()),
                board.getTribeDeckSize());
    }
}