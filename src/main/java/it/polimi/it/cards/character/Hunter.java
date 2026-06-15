package it.polimi.it.cards.character;

import it.polimi.it.enums.CharacterType;
import it.polimi.it.enums.Era;
import it.polimi.it.player.Player;

/** Immediate effect: if this hunter carries the trigger icon,
 * the player immediately takes 1 food for every hunter currently in their tribe
 */
public class Hunter extends Character {

    private final boolean hasTriggerIcon;

    public Hunter(int id, Era era, boolean hasTriggerIcon) {
        super(id, era, CharacterType.HUNTER, hasTriggerIcon ? 1 : 0);
        this.hasTriggerIcon = hasTriggerIcon;
    }

    public boolean hasTriggerIcon() { return hasTriggerIcon; }

    @Override
    public void applyImmediateEffect(Player player) {
        if (hasTriggerIcon) {
            int hunterCount = player.getTribe().countByType(CharacterType.HUNTER);
            player.addFood(hunterCount);
        }
    }
}
