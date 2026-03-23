package it.polimi.it.cards.events;

import it.polimi.it.player.Player;

import java.util.List;

/**
 * Resolution strategy for an event card (Strategy pattern).
 *
 * Each event type (Sustenance, Hunt, Shamanic Ritual, Cave Paintings)
 * implements this interface with its own calculation logic over all players.
 *
 * Events are resolved at the end of a round on the bottom row
 * (or on both rows during the final round, rulebook p.7).
 */
public interface EventStrategy {

    /**
     * Applies the event effect to all players.
     *
     * @param players all players, in turn order
     */
    void apply(List<Player> players);
}