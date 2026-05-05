package it.polimi.it.cards;

import it.polimi.it.enums.Era;

/**
 * Abstract base class for all game cards (character, building, event).
 *
 * Cards in the physical game have no printed name. They are identified
 * by their unique integer ID, which is also the natural key for network
 * messages (compact and unambiguous).
 *
 * The ID space is divided by type:
 * - Characters: 1001–1999  (era I: 1001-1099, II: 1100-1199, III: 1200-1299)
 * - Events:     2001–2099  (era I: 2001-2019, II: 2020-2039, III: 2040-2059, finals: 2090+)
 * - Buildings:  101–399    (era I: 101-106, II: 201-207, III: 301-308)
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