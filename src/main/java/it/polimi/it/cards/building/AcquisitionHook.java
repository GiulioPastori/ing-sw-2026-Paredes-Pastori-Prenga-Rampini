package it.polimi.it.cards.building;

import it.polimi.it.player.Player;
import it.polimi.it.cards.character.Character;


/**
 * Building effect that fires every time the player adds a character to
 * their tribe.
 *
 * Used for buildings that react to completing sets (e.g. a set of 6 distinct
 * character types grants 5 food) or forming pairs (e.g. a pair of inventors
 * with the same invention icon grants 3 food).
 *
 * Called after the character has already been added to the tribe, so the
 * hook can count the newly acquired character in its totals.
 */
@FunctionalInterface
public interface AcquisitionHook {

    /**
     * Applies the building effect in response to a character acquisition.
     *
     * @param player   the player who owns the building
     * @param acquired the character just added to the tribe
     */
    void apply(Player player, Character acquired);
}