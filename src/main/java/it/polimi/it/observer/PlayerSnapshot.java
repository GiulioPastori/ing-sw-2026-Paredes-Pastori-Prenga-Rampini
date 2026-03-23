package it.polimi.it.observer;

import it.polimi.it.enums.TotemColour;
import it.polimi.it.player.Player;

/**
 * Immutable snapshot of a player's visible state at a specific moment.
 *
 * GameEvents carry PlayerSnapshot instead of Player for two reasons:
 * - Listeners cannot modify the model by reading an event
 * - The snapshot represents the state at the exact moment of the event,
 *   not the current state (which may already have changed by the time
 *   a listener processes it)
 *
 * Only contains the fields the View needs to display in response to an event.
 */
public record PlayerSnapshot(
        String      name,
        TotemColour totemColour,
        int         food,
        int         prestigePoints,
        int         turnOrder
) {
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
