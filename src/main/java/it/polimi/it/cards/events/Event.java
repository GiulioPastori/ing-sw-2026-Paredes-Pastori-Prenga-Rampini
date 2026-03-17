package it.polimi.it.cards.events;

import it.polimi.it.cards.Card;
import it.polimi.it.enums.Era;
import it.polimi.it.enums.EventType;

public class Event extends Card {

    private EventType type;
    private int value;
    private EventStrategy strategy;

    public Event(EventType type, int value, EventStrategy strategy) {
        this.type = type;
        this.value = value;
        this.strategy = strategy;
    }

    public void apply(List<Player> players, Board board) {
        strategy.apply(players, board);
    }

    public int getValue() {
        return value;
    }

    public EventType getType() {
        return type;
    }
}