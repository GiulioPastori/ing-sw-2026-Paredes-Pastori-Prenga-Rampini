package it.polimi.it.cards.character;

import it.polimi.it.enums.CharacterType;
import it.polimi.it.enums.Era;

public class shaman extends Character{
    public Shaman(Era era, int icon) {
        super("Shaman", era, CharacterType.SHAMAN, icon);
    }

    @Override
    public void applyEffect(Player player) {

        /*Lo sciamano dà punti prestigio*/

        player.addPrestigePoints(icon);

    }
}
