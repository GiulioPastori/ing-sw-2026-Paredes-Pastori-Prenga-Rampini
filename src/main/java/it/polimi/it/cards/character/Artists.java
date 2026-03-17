package it.polimi.it.cards.character;

import it.polimi.it.Player.Player;
import it.polimi.it.enums.CharacterType;


public abstract class Artist extends Character {


    public Artist(String name) {

        super(name, CharacterType.ARTIST);
       }


    @Override
    public void applyEffect(Player player) {

    }



}