package it.polimi.it.cards.building;

import it.polimi.it.Player.Player;

/**
 * Building effect that contributes to the player's final score.
* Receives the full Player (not just the tribe) because
 * some scorers may need to inspect both tribe composition and remaining resources.
 */
@FunctionalInterface
public interface EndGameScorer {

    /**
     * Computes the bonus prestige points this building provides at end of game.
     *
     * @param player the player who owns the building
     * @return bonus PP (>= 0)
     */
    int compute(Player player);
}