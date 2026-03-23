package it.polimi.it.cards.building.modifier;

import it.polimi.it.player.Player;

/**
 * Capability interface implemented by buildings that modify the Shamanic Ritual event.
 *
 * Instead of ShamanicRitual comparing building IDs (brittle numeric coupling),
 * buildings with shamanic effects declare their capabilities by implementing
 * this interface. ShamanicRitual queries these capabilities without knowing
 * which specific building it is examining.
 *
 * Adding a new building with a shamanic effect does not require modifying
 * ShamanicRitual.
 *
 * Modelled effects:
 * - extraStarIcons: virtual star icon bonus before the comparison
 * - isImmuneToMinPenalty: immunity to the minimum-icons penalty
 * - doublesMaxReward: doubles the majority reward (including ties)
 */
public interface ShamanicModifier {

    /**
     * Returns the number of virtual star icons this building adds to the owner's total.
     * Added before the player comparison. Default: 0.
     *
     * @param owner the player who owns the building
     */
    default int extraStarIcons(Player owner) { return 0; }

    /**
     * Returns true if this building makes the owner immune to the penalty
     * for having the fewest star icons. Default: false.
     *
     * @param owner the player who owns the building
     */
    default boolean isImmuneToMinPenalty(Player owner) { return false; }

    /**
     * Returns true if this building doubles the majority reward for the owner
     * (including ties at the maximum). Default: false.
     *
     * @param owner the player who owns the building
     */
    default boolean doublesMaxReward(Player owner) { return false; }
}