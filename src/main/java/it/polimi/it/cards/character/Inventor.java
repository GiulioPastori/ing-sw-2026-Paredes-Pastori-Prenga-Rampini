package it.polimi.it.cards.character;

import it.polimi.it.Player.Player;
import it.polimi.it.enums.CharacterType;
import it.polimi.it.enums.Era;

public class Inventor extends Character {

    public Inventor(Era era, int icon) {
        super("Inventor", era, CharacterType.INVENTOR, icon);
    }

    public void applyEffect(Player player) {

        /* L'inventore dà bonus cibo e punti prestigio */

        player.addFood(1);
        player.addPrestigePoints(1);

    }

    @Override
    public void applyEffect(Game game) {

    }
}
