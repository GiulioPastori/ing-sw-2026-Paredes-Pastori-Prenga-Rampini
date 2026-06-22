package it.polimi.it.observer;

import it.polimi.it.Board;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Immutable snapshot of the visible board state at a given moment.
 *
 * <p>Carried by events that change the board, such as
 * {@link GameEvent.TotemPlaced}. Adding a new field to this record affects
 * only events that already include a {@link BoardSnapshot}; consumers that
 * ignore the field continue to work.
 *
 * <p>Implements {@link Serializable} so it can be passed inside Message
 * objects over RMI and via Java object serialisation over sockets.
 *
 * @param offerTiles visible offer tiles, including free and occupied tiles
 * @param turnOrderTile snapshot of the Turn Order Tile
 * @param tribeDeckSize number of cards remaining in the tribe deck
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
