package it.polimi.it.cards.building;

import it.polimi.it.player.Player;
import it.polimi.it.cards.character.Character;


/** Building effect that fires every time the player adds a character to
 * their tribe. Called after the character has already been added to the tribe, so the
 * hook can count the newly acquired character in its totals.
 */
@FunctionalInterface
public interface AcquisitionHook {

    /**
     * Applies the building hook after a character has been acquired.
     *
     * @param player   the player who owns the building
     * @param acquired the character just added to the tribe
     */
    void apply(Player player, Character acquired);
}
