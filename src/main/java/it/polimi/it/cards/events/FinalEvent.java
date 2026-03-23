package it.polimi.it.cards.events;

import it.polimi.it.enums.Era;
import it.polimi.it.enums.EventType;

public class FinalEvent extends Event{

    public FinalEvent(int id, EventType type, int value, EventStrategy strategy) {
        super(id, Era.ERA_III, type, value, strategy);
    }
}
