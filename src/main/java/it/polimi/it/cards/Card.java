package it.polimi.it.cards;

import it.polimi.it.enums.Era;

public abstract class Card {
    protected String name;
    protected Era era;

    public Card(String name, Era era) {
        this.name = name;
        this.era = era;
    }

    public String getName() {

        return name;
    }

    public Era getEra() {

        return era;
    }

    public abstract void applyEffect(Game game);
}
