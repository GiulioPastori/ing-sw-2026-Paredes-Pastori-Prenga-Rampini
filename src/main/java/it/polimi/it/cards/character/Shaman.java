package it.polimi.it.cards.character;


import it.polimi.it.Board;
import it.polimi.it.Player.Player;
import it.polimi.it.enums.CharacterType;
import it.polimi.it.enums.Era;

public class Shaman extends Character {

    public Shaman(String name, Era era, CharacterType type, int shamanIcons, int basePrestige, boolean totemIcon) {
        super(name, era, CharacterType.SHAMAN,shamanIcons,basePrestige, totemIcon);
    }


    public int getShamanIcons() {

        return getIcon();
    }

    @Override
    protected void onAcquire(Player player, Board board) {
        // nessun effetto immediato
    }

}