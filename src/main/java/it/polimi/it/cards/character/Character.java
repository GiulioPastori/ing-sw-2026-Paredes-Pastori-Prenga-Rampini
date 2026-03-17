package it.polimi.it.cards.character;

import it.polimi.it.Game;
import it.polimi.it.Player.Player;
import it.polimi.it.cards.Card;
import it.polimi.it.enums.CharacterType;
import it.polimi.it.enums.Era;

public abstract class Character extends Card {
    private CharacterType typeOfCharacter;
    private int icon = 0;

    public Character(String name, Era era, CharacterType typeOfCharacter, int icon) {
            super(name, era);
            this.typeOfCharacter = typeOfCharacter;
            this.icon = icon;
     }

     public CharacterType getTypeOfCharacter() {

        return this.typeOfCharacter;
    }

  //restituisce quante icone ha la carta//
    public int getIcon() {

        return this.icon;
    }

    public abstract void applyEffect(Player player);


    public void applyEffect(Game game) {
        Player currentPlayer= game.getCurrentPlayer();
        applyEffect(currentPlayer);

    }
}