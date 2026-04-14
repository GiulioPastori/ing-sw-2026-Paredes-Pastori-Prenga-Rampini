package it.polimi.it.observer;

import it.polimi.it.enums.TotemColour;
import it.polimi.it.player.Player;

import java.io.Serial;
import java.io.Serializable;

/**
 * Immutable snapshot of a player's visible state at a specific moment.
 *
 * Implements Serializable so it can be passed inside Message objects
 * over RMI and via Java object serialisation over sockets.
 */
public record PlayerSnapshot(
        String      name,
        TotemColour totemColour,
        int         food,
        int         prestigePoints,
        int         turnOrder
) implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    /** Creates a snapshot of the player in their current state. */
    public static PlayerSnapshot of(Player player) {
        return new PlayerSnapshot(
                player.getName(),
                player.getTotemColour(),
                player.getFood(),
                player.getPrestigePoints(),
                player.getTurnOrder()
        );
    }
}
