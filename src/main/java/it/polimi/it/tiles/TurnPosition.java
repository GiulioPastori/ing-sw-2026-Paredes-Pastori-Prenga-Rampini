package it.polimi.it.tiles;

public class TurnPosition {
    private final int position;
    private final int foodTokens; // food bonus when landing here
    private final int penalties;  // PP penalty if this is the last space (0 or -2)
    private Totem placedTotem;

    public TurnPosition(int position, int foodTokens, int penalties) {
        this.position    = position;
        this.foodTokens  = foodTokens;
        this.penalties   = penalties;
        this.placedTotem = null;
    }

    public boolean isFree()        { return placedTotem == null; }
    public void    place(Totem t)  { this.placedTotem = t; }
    public Totem   free()          { Totem t = placedTotem; placedTotem = null; return t; }

    public int   getPosition()    { return position; }
    public int   getFoodTokens()  { return foodTokens; }
    public int   getPenalties()   { return penalties; }
    public Totem getPlacedTotem() { return placedTotem; }
}
