package it.polimi.it.cards.building.modifier;

import it.polimi.it.player.Player;

/** Capability interface implemented by buildings that modify the Shamanic Ritual event. */
public interface ShamanicModifier {

    /**
     * Returns the number of virtual star icons this building adds to the owner's total.
     * Added before the player comparison. Default: 0.
     * @param owner the player who owns the building
     */
    default int extraStarIcons(Player owner) { return 0; }

    /**
     * Returns true if this building makes the owner immune to the penalty
     * for having the fewest star icons. Default: false.
     * @param owner the player who owns the building
     */
    default boolean isImmuneToMinPenalty(Player owner) { return false; }

    /**
     * Returns true if this building doubles the majority reward for the owner. Default: false.
     * @param owner the player who owns the building
     */
    default boolean doublesMaxReward(Player owner) { return false; }
}