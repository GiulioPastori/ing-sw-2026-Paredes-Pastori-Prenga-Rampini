package it.polimi.it.observer;

import it.polimi.it.enums.TotemColour;
import it.polimi.it.tiles.TurnOrderTile;
import it.polimi.it.tiles.TurnPosition;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Immutable snapshot of the Turn Order Tile.
 *
 * <p>Carries everything the view needs to render the tile: the player count
 * selects the visual variant, while positions describe every space from first
 * to last. Free spaces have a {@code null} totem.
 *
 * <p>Implements {@link Serializable} so it can be passed inside Message
 * objects over RMI and via Java object serialisation over sockets.
 *
 * @param numPlayers number of players in the game
 * @param positions turn-order positions in placement order
 */
public record TurnOrderTileSnapshot(
        int                numPlayers,
        List<PositionSnap> positions
) implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    /**
     * Single position on the Turn Order Tile.
     *
     * @param position position number; 1 is first to play next round
     * @param foodBonus food granted when landing on this space; 0 if none
     * @param isLast true for the last space
     * @param placedTotem totem currently on this space, or {@code null} if free
     */
    public record PositionSnap(
            int         position,
            int         foodBonus,
            boolean     isLast,
            TotemColour placedTotem
    ) implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
    }

    /** Builds a snapshot from the live tile. */
    public static TurnOrderTileSnapshot of(TurnOrderTile tile) {
        List<PositionSnap> snaps = tile.getPositions().stream()
                .map(p -> new PositionSnap(
                        p.getPosition(),
                        p.getFoodBonus(),
                        p.isLastPosition(),
                        p.isFree() ? null : p.getPlacedTotem()))
                .toList();
        return new TurnOrderTileSnapshot(tile.getNumPlayers(), snaps);
    }
}
