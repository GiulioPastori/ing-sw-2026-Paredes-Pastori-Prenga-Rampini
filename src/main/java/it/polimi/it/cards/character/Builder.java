package it.polimi.it.cards.character;

import it.polimi.it.Player.Player;
import it.polimi.it.Player.Tribe;
import it.polimi.it.enums.CharacterType;
import it.polimi.it.enums.Era;

import java.util.Locale;

public abstract class Builder extends Character {
    private final int buildingsDiscount;
    private final int endPrestigePoints;

    public Builder(String name, Era era, int buildingsDiscount, int endPrestigePoints) {
        super(name, era, CharacterType.BUILDER);
        this.buildingsDiscount = buildingsDiscount;
        this.endPrestigePoints = endPrestigePoints;
    }

    public int getbuildingsDiscount() {

        return buildingsDiscount;
    }


    @Override
    public void applyEffect( Player player){
        Tribe tribe= player.getTribe();
        Object playerTribe = null;
        playerTribe.addBuildingDiscount(this.buildingsDiscount);
        playerTribe.addPrestigePoints(this.endGamePoints());
    }



    public int endGamePoints() {

     return endPrestigePoints;
    }

}