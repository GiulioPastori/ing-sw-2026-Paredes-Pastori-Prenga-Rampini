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
        // Passiamo endPrestigePoints come basePrestige alla classe madre
        super(name, era, CharacterType.BUILDER, 0, endPrestigePoints);
        this.buildingsDiscount = buildingsDiscount;
        this.endPrestigePoints = endPrestigePoints;
    }

    public int getBuildingsDiscount() {

        return buildingsDiscount;
    }


    @Override
    protected void onAcquire(Player player, Board board) {
        // nessun effetto immediato
    }

    /**
     * I punti di fine partita sono già memorizzati in basePrestige,
     * sovrascrivo
     */
    @Override
    public int calculateFinalPoints(Player player) {

        return 0;
    }


}