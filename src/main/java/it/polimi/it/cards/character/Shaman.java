package it.polimi.it.cards.character;

import it.polimi.it.enums.CharacterType;
import it.polimi.it.enums.Era;
import it.polimi.it.player.Player;

/** Shamans show 1–3 stars icons.
  No immediate effect; no direct contribution to the final score.*/
public class Shaman extends Character {

    public Shaman(int id, Era era, int starIcons) {
        super(id, era, CharacterType.SHAMAN, starIcons);
        if (starIcons < 1 || starIcons > 3) {
            throw new IllegalArgumentException(
                    "A shaman must have between 1 and 3 star icons, got: " + starIcons);
        }
    }

    /** Returns the number of star icons on this shaman. */
    public int getStarIcons() { return getIconCount(); }

    @Override
    public void applyImmediateEffect(Player player) { /* no immediate effect */ }
}