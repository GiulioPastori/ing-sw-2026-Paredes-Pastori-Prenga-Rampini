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
 * Carries everything the view needs to render the tile:
 * { numPlayers} — drives the visual variant (2-, 3-, 4-, 5-player
 *       layout has different bonus spaces).
 * { positions} — one entry per space, in placement order
 *       (first to last). Free spaces have a {null} totem.
 *
 * Implements { Serializable} so it can be passed inside Message
 * objects over RMI and via Java object serialisation over sockets.
 */
public record TurnOrderTileSnapshot(
        int                numPlayers,
        List<PositionSnap> positions
) implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    /**
     * Single position on the Turn Order Tile.
     *
     * position=position number (1 = first to play next round)
     * foodBonus=food granted when landing on this space (0 if none)
     * isLast =true for the last space (food penalty / -2 PP)
     * placedTotem=totem currently on this space, or { null} if free
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