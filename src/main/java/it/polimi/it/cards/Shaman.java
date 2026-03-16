package it.polimi.it.cards;


import it.polimi.it.enums.CharacterType;

public class Shaman extends Character {
    private final int shamanicIconsCount;

    public Shaman(String name, int era, int shamanicIconsCount) {
        super(name, era, CharacterType.SHAMAN);
        this.shamanicIconsCount = shamanicIconsCount;
    }

    public int getShamanicIconsCount() {
        return shamanicIconsCount;
    }
}