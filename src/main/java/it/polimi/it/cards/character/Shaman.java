package it.polimi.it.cards.character;


import it.polimi.it.Board;
import it.polimi.it.Player.Player;
import it.polimi.it.enums.CharacterType;
import it.polimi.it.enums.Era;

public class Shaman extends Character {
    private int shamanIcons;
    private int icon;

    public Shaman(String name, Era era, CharacterType type, int icon, int shamanIcons) {
        super(name, era, type.SHAMAN, 0, 0);
        this.shamanIcons = shamanIcons;
    }


    public int getShamanIcons() {

        return this.shamanIcons;
    }

    public void setShamanIcons(int shamanIcons) {

        this.shamanIcons = shamanIcons;
    }



    @Override
    protected void onAcquire(Player player, Board board) {
        // nessun effetto immediato
    }

    @Override
    public int getIcon() {

        return this.icon;
    }

    @Override
    public void onEvent(Player player, Board board) {
        // usato nel calcolo rituale (gestito da Event)
    }

}