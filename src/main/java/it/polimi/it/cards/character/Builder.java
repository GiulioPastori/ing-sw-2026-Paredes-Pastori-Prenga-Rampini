package it.polimi.it.cards.character;

import it.polimi.it.enums.CharacterType;
import it.polimi.it.enums.Era;
import it.polimi.it.player.Player;

/**
 * During the game: reduces the food cost of every building acquired.
 * End of game: contributes fixed prestige points.
 */
public class Builder extends Character {

    private final int buildingDiscount;
    private final int endGamePoints;

    public Builder(int id, Era era, int buildingDiscount, int endGamePoints) {
        super(id, era, CharacterType.BUILDER, 0);
        this.buildingDiscount = buildingDiscount;
        this.endGamePoints    = endGamePoints;
    }

    public int getBuildingDiscount() { return buildingDiscount; }
    public int getEndGamePoints()    { return endGamePoints; }

    @Override
    public void applyImmediateEffect(Player player) { /* no immediate effect */ }
}