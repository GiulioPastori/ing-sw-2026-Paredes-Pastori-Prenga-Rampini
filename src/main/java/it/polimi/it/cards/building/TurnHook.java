package it.polimi.it.cards.building;

import it.polimi.it.player.Player;
import it.polimi.it.tiles.TurnPosition;

/**
 * Building effect that fires when the player repositions their totem
 * on the Turn Order Tile after resolving their offer tile action.
 *
 * Currently used only by the building that grants 1 extra food when
 * the player lands on a food-bonus space.
 */
@FunctionalInterface
public interface TurnHook {

    /**
     * Applies the building effect in response to totem placement.
     *
     * @param player   the player who owns the building
     * @param position the Turn Order Tile space that was occupied
     */
    void apply(Player player, TurnPosition position);
}
