package it.polimi.it.cards;

import it.polimi.it.enums.CharacterType;
import it.polimi.it.enums.Era;

public class Character extends Card {
    private CharacterType typeOfCharacter;
    private int icon;

    public Character(String name, Era era, CharacterType typeOfCharacter, int icon) {
            super(name, era);
            this.typeOfCharacter = type;
            this.icon = icon;
     }

     public CharacterType getTypeOfCharacter() {
        return typeOfCharacter;
    }

    public int getIcon() {
        return icon;
    }


    @Override
    public void applyEffect(Game game) {

    }
}