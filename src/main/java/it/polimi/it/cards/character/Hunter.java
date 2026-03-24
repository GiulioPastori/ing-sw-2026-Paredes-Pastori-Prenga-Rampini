package it.polimi.it.cards.character;

import it.polimi.it.enums.CharacterType;
import it.polimi.it.enums.Era;
import it.polimi.it.player.Player;

/**
 * Character card: Hunter.
 *
 * Immediate effect (rulebook p.5): if this hunter carries the trigger icon,
 * the player immediately takes 1 food for every hunter currently in their tribe
 * (including this one, which has already been added).
 *
 * Hunt event (rulebook p.6): for each hunter in the tribe the player takes
 * 1 food and gains the prestige points shown on the event card.
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
