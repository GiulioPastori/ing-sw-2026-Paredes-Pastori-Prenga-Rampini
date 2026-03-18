package it.polimi.it.cards.character;


import it.polimi.it.Board;
import it.polimi.it.Player.Player;
import it.polimi.it.Player.Tribe;
import it.polimi.it.enums.CharacterType;
import it.polimi.it.enums.Era;

public class Inventor extends Character {
    private int inventionIcon;

    public Inventor(String name, Era era, int inventionIcon) {
        super(name, era, CharacterType.INVENTOR,0);
        this.inventionIcon = inventionIcon;
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

    @Override
    protected void onAcquire(Player player, Board board) {
        // Nessun effetto immediato
        // ma il metodo DEVE esistere
    }


}