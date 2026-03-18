package it.polimi.it.cards.character;


import it.polimi.it.Board;
import it.polimi.it.Player.Player;
import it.polimi.it.Player.Tribe;
import it.polimi.it.enums.CharacterType;
import it.polimi.it.enums.Era;

public class Gatherer extends Character {
    private int sustenanceDiscount;

    public Gatherer(String name, Era era, CharacterType type, int icon, int sustenanceDiscount) {
        super(name, era, type.GATHERER, 0, 0);
        this.sustenanceDiscount = sustenanceDiscount;
    }


    @Override
    protected void onAcquire(Player player, Board board) {
        player.addFood(sustenanceDiscount);
    }


    public int getSustenanceDiscount() {

        return this.sustenanceDiscount;
    }
    @Override
    public void onEvent(Player player, Board board) {
        // può ridurre penalità di sustenance (gestito in Event)
    }
    public void setSustenanceDiscount(int sustenanceDiscount) {

        this.sustenanceDiscount = sustenanceDiscount;
    }

}
