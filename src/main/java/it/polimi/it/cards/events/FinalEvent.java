package it.polimi.it.cards.events;

import it.polimi.it.cards.events.Event;
import it.polimi.it.enums.Era;
import it.polimi.it.enums.EventType;

/**
 * Final event card. Era III. Placed at the bottom of the tribe deck
 * and resolved during the last round together with all visible events
 */
public class FinalEvent extends Event {

    public FinalEvent(int id, EventType type, int value, EventStrategy strategy) {
        super(id, Era.ERA_III, type, value, strategy);
    }
}
