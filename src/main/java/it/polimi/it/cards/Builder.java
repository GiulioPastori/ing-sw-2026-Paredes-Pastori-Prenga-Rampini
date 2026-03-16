package it.polimi.it.cards;

import it.polimi.it.enums.CharacterType;

public class Builder extends Character {
    private final int constructionDiscount;
    private final int endPrestigePoints;

    public Builder(String name, int era, int constructionDiscount, int endPrestigePoints) {
        super(name, era, CharacterType.BUILDER);
        this.constructionDiscount = constructionDiscount;
        this.endPrestigePoints = endPrestigePoints;
    }

    public int getConstructionDiscount() {
        return constructionDiscount;
    }

    public int endGamePoints() {
        return endPrestigePoints;
    }
}