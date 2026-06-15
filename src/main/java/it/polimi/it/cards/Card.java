package it.polimi.it.cards;

import it.polimi.it.enums.Era;

/** Abstract base class for all game cards (character, building, event).*/
public abstract class Card {
    private int minPlayers = 2; // default: tutte le configurazioni

    public int getMinPlayers() { return minPlayers; }
    public void setMinPlayers(int minPlayers) { this.minPlayers = minPlayers; }

    /** Unique integer identifier for this card. */
    protected final int id;

    /** Era */
    protected final Era era;

    public Card(int id, Era era) {
        this.id  = id;
        this.era = era;
    }

    public int  getId()  { return id; }
    public Era  getEra() { return era; }


    @Override
    public String toString() {
        return getClass().getSimpleName() + "#" + id;
    }
}