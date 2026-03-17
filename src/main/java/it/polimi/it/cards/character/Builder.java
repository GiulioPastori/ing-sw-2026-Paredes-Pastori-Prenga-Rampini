package it.polimi.it.cards.character;

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

    public static class Inventor extends java.lang.Character {
        private String inventionIcon;

        public Inventor(String nome, int era, String icona) {
            super(nome, era, CharacterType.INVENTORS);
            this.inventionIcon = icon;
        }

        public String getinventionIcon() {
            return inventionIcon;
        }
    }

    public static class Shaman extends Character {
        private final int shamanicIconsCount;

        public Shaman(String name, int era, int shamanicIconsCount) {
            super(name, era, CharacterType.SHAMAN);
            this.shamanicIconsCount = shamanicIconsCount;
        }

        public int getShamanicIconsCount() {
            return shamanicIconsCount;
        }
    }
}