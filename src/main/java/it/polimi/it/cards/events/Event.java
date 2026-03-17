package it.polimi.it.cards.events;

import it.polimi.it.cards.Card;
import it.polimi.it.enums.Era;
import it.polimi.it.enums.EventType;

public class Event extends Card {


    private EventType typeOfEvent;
    private int valueEra;

    public Event(String name, Era era, EventType typeOfEvent, int valueEra) {

        super(name, era);

        this.typeOfEvent = typeOfEvent;
        this.valueEra = valueEra;
    }



    public EventType applyEvent(List<Player> players) {

        if (players == null) {
                    return;
        }

        for (Player p : players) {

            applyEffect(p);

        }

        public EventType getTypeOfEvent() {
            return typeOfEvent;
        }

        public int getValueEra() {
            return valueEra;
        }


        @Override
        public void applyEffect(Game game) {


    }
}
