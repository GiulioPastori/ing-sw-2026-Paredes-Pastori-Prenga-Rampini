package it.polimi.it.cards;

import it.polimi.it.enums.CharacterType;


public class Artist extends Character {


    public Artist(String name) {
        super(name, CharacterType.ARTIST);
       }


    @Override
    public void applyEffect(Player player) {

    }

    private int calculatePaintingPoints() {
    }


}