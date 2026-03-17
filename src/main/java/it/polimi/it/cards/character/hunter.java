package it.polimi.it.cards.character;


import it.polimi.it.enums.CharacterType;


public class Hunter extends Character {


    private final int foodBonus;
    private final int huntEventBonus; //

    public Hunter(String name, int era, int foodBonus, int huntEventBonus) {
        super(name, era, CharacterType.HUNTER);
        this.foodBonus = foodBonus;
        this.huntEventBonus = huntEventBonus;
    }

}
