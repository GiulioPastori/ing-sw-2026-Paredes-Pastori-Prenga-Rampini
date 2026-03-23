package it.polimi.it.cards.character;

import it.polimi.it.Board;
import it.polimi.it.player.Player;
import it.polimi.it.enums.CharacterType;
import it.polimi.it.enums.Era;

public class Hunter extends Character {
    private final int huntBonus;
    private final boolean hasIcon;

    public Hunter(String name, Era era, int icon, int basePrestige, int huntBonus, boolean hasIcon, boolean totemIcon) {
        // Passiamo icon e basePrestige a  Character
        super(name, era, CharacterType.HUNTER, icon, basePrestige, totemIcon);
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
