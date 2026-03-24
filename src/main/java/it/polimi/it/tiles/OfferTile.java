package it.polimi.it.tiles;

import it.polimi.it.enums.TotemColour;

/**
 * A single tile on the Offer Track.
 *
 * Each tile is identified by a letter (A–G) and specifies:
 * - how many cards the player may take from the top row
 * - how many cards the player may take from the bottom row
 * - the minimum number of players required for the tile to be active
 *
 * Exception: tile A (5-player games only) provides 3 food instead of card draws
 * ({@link #isFoodOnly()} returns true, rulebook p.4).
 *
 * Only one totem can be placed on each tile at a time.
 */
public class OfferTile {

    /** Identifying letter of the tile (A–G), determines position on the track. */
    private final char letter;

    /** Minimum number of players for this tile to be active. */
    private final int minPlayers;

    /** Number of cards the player may take from the top row. */
    private final int topRowCards;

    /** Number of cards the player may take from the bottom row. */
    private final int bottomRowCards;

    /**
     * If true, this tile provides 3 food instead of card draws.
     * Only tile A in 5-player games (rulebook p.4).
     */
    private final boolean isFoodOnly;

    /** Colour of the totem currently placed on this tile; null if free. */
    private TotemColour placedTotem;

    public OfferTile(char letter, int minPlayers, int topRowCards,
                     int bottomRowCards, boolean isFoodOnly) {
        this.letter         = letter;
        this.minPlayers     = minPlayers;
        this.topRowCards    = topRowCards;
        this.bottomRowCards = bottomRowCards;
        this.isFoodOnly     = isFoodOnly;
        this.placedTotem    = null;
    }

    // Totem management

    /** Returns true if no totem is currently placed on this tile. */
    public boolean isFree() { return placedTotem == null; }

    /**
     * Places a totem on this tile.
     *
     * @param totem colour of the totem to place
     * @throws IllegalStateException if the tile is already occupied
     */
    public void placeTotem(TotemColour totem) {
        if (!isFree()) throw new IllegalStateException("Tile " + letter + " is already occupied.");
        this.placedTotem = totem;
    }

    /**
     * Removes the totem from this tile and returns it.
     * Called when the player repositions their totem on the Turn Order Tile.
     *
     * @return colour of the removed totem
     * @throws IllegalStateException if the tile was already free
     */
    public TotemColour removeTotem() {
        if (isFree()) throw new IllegalStateException("Tile " + letter + " is already free.");
        TotemColour removed = placedTotem;
        placedTotem = null;
        return removed;
    }

    // Getters

    public char        getLetter()         { return letter; }
    public int         getMinPlayers()     { return minPlayers; }
    public int         getTopRowCards()    { return topRowCards; }
    public int         getBottomRowCards() { return bottomRowCards; }
    public boolean     isFoodOnly()        { return isFoodOnly; }
    public TotemColour getPlacedTotem()    { return placedTotem; }
}


