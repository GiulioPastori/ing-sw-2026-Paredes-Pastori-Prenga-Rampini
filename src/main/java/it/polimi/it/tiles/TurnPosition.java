package it.polimi.it.tiles;


import it.polimi.it.Player.Player;import it.polimi.it.enums.TotemColour;

public class TurnPosition {
    private final int position;
    private final int foodTokens; // food bonus when landing here
    private final int penalties;  // PP penalty if this is the last space (0 or -2)
    private Player placedTotem;

    public TurnPosition(int position, int foodTokens, int penalties) {
        this.position    = position;
        this.foodTokens  = foodTokens;
        this.penalties   = penalties;
        this.placedTotem = null;
    }

    public boolean isFree()        { return placedTotem == null; }
    public void    place(Player p)  { this.placedTotem = p; }
    public Player   free()          { Player p = placedTotem; placedTotem = null; return p; }

    public int   getPosition()    { return position; }
    public int   getFoodTokens()  { return foodTokens; }
    public int   getPenalties()   { return penalties; }
    public Player getPlacedTotem() { return placedTotem; }
}
