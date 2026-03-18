package it.polimi.it.cards.character;

import it.polimi.it.Board;
import it.polimi.it.Player.Player;
import it.polimi.it.Player.Tribe;
import it.polimi.it.enums.CharacterType;
import it.polimi.it.enums.Era;

public class Hunter extends Character {
    private int huntBonus;
    private boolean hasIcon;

    public Hunter(String name, Era era, CharacterType type,int huntBonus, boolean hasIcon) {
        super(name, era, CharacterType.HUNTER, 0);
        this.huntBonus = huntBonus;
        this.hasIcon= hasIcon;
    }

    @Override
    protected void onAcquire(Player player, Board board) {
        // produzione immediata di cibo
        player.addFood(player.getFood());
    }

    public int getHuntBonus() {

        return this.huntBonus;
    }

    public void setHuntBonus(int huntBonus) {

        this.huntBonus = huntBonus;
    }


}
