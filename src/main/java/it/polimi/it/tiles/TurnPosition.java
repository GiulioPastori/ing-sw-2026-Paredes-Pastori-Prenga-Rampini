package it.polimi.it.tiles;

import it.polimi.it.enums.TotemColour;

/**
 * A single space on the Turn Order Tile.
 * Each space has a fixed position (1 = top) and a food bonus or penalty
 * - 1st space: +3 food
 * - 2nd space: +1 food
 * - middle spaces: no bonus
 * - last space: pay 1 food (or lose 2 PP if no food available)
 * The totem colour identifies which player is occupying the space.
 */
public class TurnPosition {

    private final int     position;
    private final int     foodBonus;
    private final boolean isLastPosition;
    private TotemColour   placedTotem;

    /** position= 1-based position index
     * foodBonus=food bonus for landing here (0 if none)
     * isLastPosition true only for the last space
     */
    public TurnPosition(int position, int foodBonus, boolean isLastPosition) {
        this.position       = position;
        this.foodBonus      = foodBonus;
        this.isLastPosition = isLastPosition;
        this.placedTotem    = null;
    }

    /** Returns true if this space is not occupied by a totem. */
    public boolean isFree() { return placedTotem == null; }

    /** Places a totem on this space.
     * totem colour of the totem to place
     * IllegalStateException if the space is already occupied */
    public void place(TotemColour totem) {
        if (!isFree()) throw new IllegalStateException("Space " + position + " is already occupied.");
        this.placedTotem = totem;
    }

    /** Removes the totem from this space and returns it.
     * return the removed totem colour
     * IllegalStateException if the space was already free
     */
    public TotemColour free() {
        if (isFree()) throw new IllegalStateException("Space " + position + " is already free.");
        TotemColour removed = placedTotem;
        placedTotem = null;
        return removed;
    }

    public int         getPosition()    { return position; }
    public int         getFoodBonus()   { return foodBonus; }
    public boolean     isLastPosition() { return isLastPosition; }
    public TotemColour getPlacedTotem() { return placedTotem; }
}