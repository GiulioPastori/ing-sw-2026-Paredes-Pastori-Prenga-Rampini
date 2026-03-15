package it.polimi.it.cards;

import it.polimi.it.enums.Era;
import it.polimi.it.enums.EventType;

public class Event extends Card {

        private EventType type;

        public Event(String name, Era era, EventType type) {

            super(name, era);

            this.type = type;

        }

        public EventType getType() {
            return type;
        }

        @Override
        public void applyEffect(Game game) {


    }
}
