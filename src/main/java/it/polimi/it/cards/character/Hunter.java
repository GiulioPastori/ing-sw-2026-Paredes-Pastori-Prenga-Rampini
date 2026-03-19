package it.polimi.it.cards.character;

import it.polimi.it.Board;
import it.polimi.it.Player.Player;
import it.polimi.it.enums.CharacterType;
import it.polimi.it.enums.Era;

public class Hunter extends Character {
    private final int huntBonus;
    private final boolean hasIcon;

    public Hunter(String name, Era era, int icon, int basePrestige, int huntBonus, boolean hasIcon) {
        // Passiamo icon e basePrestige a  Character
        super(name, era, CharacterType.HUNTER, icon, basePrestige);
        this.huntBonus = huntBonus;
        this.hasIcon = hasIcon;
    }

    @Override
    protected void onAcquire(Player player, Board board) {
        // Aggiunge il cibo fisso indicato sulla carta
        player.addFood(this.huntBonus);
    }

    public int getHuntBonus() {
        return this.huntBonus;
    }

    public boolean hasIcon() {
        return this.hasIcon;
    }
}
