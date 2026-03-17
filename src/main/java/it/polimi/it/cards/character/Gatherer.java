package it.polimi.it.cards.character;

import it.polimi.it.enums.CharacterType;

public class Gatherer extends Character {
    private static final int FOOD_DISCOUNT = 3;

    public Gatherer(String name, int era) {
        super(name, era, CharacterType.GATHERER);
    }

    public int getFoodDiscount() {
        return FOOD_DISCOUNT;
    }
}
