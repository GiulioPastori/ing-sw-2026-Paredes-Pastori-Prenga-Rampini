package it.polimi.it.cards.events;

import it.polimi.it.cards.Card;
import it.polimi.it.enums.Era;
import it.polimi.it.enums.EventType;
import it.polimi.it.player.Player;

import java.util.List;

/**
 * Event card.
 *
 * There are 4 event types (Sustenance, Hunt, Shamanic Ritual, Cave Paintings),
 * one per type per era (12 event cards total in the tribe deck).
 * Event cards can never be taken by players (rulebook p.4).
 * Resolution logic is delegated to an EventStrategy.
 */
public class Event extends Card {

    private final EventType     type;
    private final int           value;
    private final EventStrategy strategy;

    public Event(int id, Era era, EventType type, int value, EventStrategy strategy) {
        super(id, era);
        this.type     = type;
        this.value    = value;
        this.strategy = strategy;
    }

    /** Resolves this event by applying the strategy to all players. */
    public void resolve(List<Player> players) {
        strategy.apply(players);
    }

    public EventType getType()  { return type; }
    public int       getValue() { return value; }
}