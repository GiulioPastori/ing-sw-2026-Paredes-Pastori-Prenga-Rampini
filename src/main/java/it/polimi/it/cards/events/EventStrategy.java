package it.polimi.it.cards.events;

import it.polimi.it.player.Player;

import java.util.List;

/** Strategy pattern
 * Each event type implements this interface with its own calculation logic over all players.*/

public interface EventStrategy {

    /** Applies the event effect to all players.
     * @param players all players, in turn order
     */
    void apply(List<Player> players);
}