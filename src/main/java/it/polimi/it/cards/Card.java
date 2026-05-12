package it.polimi.it.cards;

import it.polimi.it.enums.Era;

/**
 * Abstract base class for all game cards (character, building, event).
 *
 * Cards in the physical game have no printed name. They are identified
 * by their unique integer ID, which is also the natural key for network
 * messages (compact and unambiguous).

 */
public abstract class Card {
    private int minPlayers = 2; // default: tutte le configurazioni

    public int getMinPlayers() { return minPlayers; }
    public void setMinPlayers(int minPlayers) { this.minPlayers = minPlayers; }
    /** Unique integer identifier for this card. Used as the network key. */
    protected final int id;

    /** Era this card belongs to (I, II, or III). */
    protected final Era era;

    public Card(int id, Era era) {
        this.id  = id;
        this.era = era;
    }

    public int  getId()  { return id; }
    public Era  getEra() { return era; }

    /**
     * Human-readable label for logs and debug output.
     * Not used as an identifier anywhere in the model.
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + "#" + id;
    }
}