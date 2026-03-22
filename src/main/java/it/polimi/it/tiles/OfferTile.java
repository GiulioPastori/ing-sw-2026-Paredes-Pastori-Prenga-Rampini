package it.polimi.it.tiles;

import it.polimi.it.enums.TotemColour;

public class OfferTile {

    private final char letter;
    private final int minPlayers;
    private final int topRowCards;
    private final int bottomRowCards;
    private final int foodBonus;       // food taken when returning totem to Turn Order tile
    private final boolean isFoodOnly;  // true only for tile A (take 3 food, no card draw)

    private TotemColour placedTotem;

    public OfferTile(char letter, int minPlayers, int topRowCards, int bottomRowCards,
                     int foodBonus, boolean isFoodOnly) {
        this.letter = letter;
        this.minPlayers = minPlayers;
        this.topRowCards = topRowCards;
        this.bottomRowCards = bottomRowCards;
        this.foodBonus = foodBonus;
        this.isFoodOnly = isFoodOnly;
        this.placedTotem = null;
    }

    // --- Totem placement ---

    public boolean isFree() {
        return placedTotem == null;
    }

    public void placeTotem(TotemColour totem) {
        if (!isFree()) throw new IllegalStateException("Tile " + letter + " is already occupied.");
        this.placedTotem = totem;
    }

    public TotemColour removeTotem() {
        if (isFree()) throw new IllegalStateException("Tile " + letter + " has no totem.");
        TotemColour t = placedTotem;
        placedTotem = null;
        return t;
    }

    // --- Getters ---

    public char    getLetter()         { return letter; }
    public int     getMinPlayers()     { return minPlayers; }
    public int     getTopRowCards()    { return topRowCards; }
    public int     getBottomRowCards() { return bottomRowCards; }
    public int     getFoodBonus()      { return foodBonus; }
    public boolean isFoodOnly()        { return isFoodOnly; }
    public TotemColour   getPlacedTotem()    { return placedTotem; }
}
