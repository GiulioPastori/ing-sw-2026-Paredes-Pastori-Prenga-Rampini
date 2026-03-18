package it.polimi.it.cards.character;

import it.polimi.it.Board;
import it.polimi.it.Player.Player;
import it.polimi.it.Player.Tribe;
import it.polimi.it.enums.CharacterType;
import it.polimi.it.enums.Era;

public class Builder extends Character {
    private final int buildingsDiscount;
    private final int endPrestigePoints;

    public Builder(String name, Era era,CharacterType type, int buildingsDiscount, int endPrestigePoints) {
        super(name, era, CharacterType.BUILDER,0, 0);
        this.buildingsDiscount = buildingsDiscount;
        this.endPrestigePoints = endPrestigePoints;
    }

    public int getbuildingsDiscount() {

        return buildingsDiscount;
    }


    @Override
    protected void onAcquire(Player player, Board board) {
        // nessun effetto immediato
    }



    public int endGamePoints() {

     return endPrestigePoints;
    }

}