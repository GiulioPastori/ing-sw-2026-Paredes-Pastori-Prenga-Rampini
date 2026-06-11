package it.polimi.it.cards.building;

import it.polimi.it.player.Player;

/** Building effect that contributes to the player's final score.*/
@FunctionalInterface
public interface EndGameScorer {

    /** @param player the player who owns the building
     * @return bonus PP (>= 0) */
    int compute(Player player);
}