package it.polimi.it.cards.character;

import it.polimi.it.Player.Player;
import it.polimi.it.Player.Tribe;
import it.polimi.it.enums.CharacterType;
import it.polimi.it.enums.Era;


public class Inventor extends Character {
    private int inventionIcon;

    public Inventor(String name, Era era, CharacterType type, int inventionIcon) {
        super(name, era, type);
        this.inventionIcon = inventionIcon;
    }

    @Override
    public void applyEffect(Player player) {
        if (player != null && player.getTribe() != null) {
            Tribe tribe = player.getTribe();
            tribe.addInventionIcon(this.inventionIcon);
            tribe.addCharacter(this);
        }
    }

    public int getInventionIcon() {
        return this.inventionIcon;
    }

    public void setInventionIcon(int inventionIcon) {
        this.inventionIcon = inventionIcon;
    }

    public CharacterType getTypeOfCharacter() {
        return CharacterType.INVENTOR;
    }


}