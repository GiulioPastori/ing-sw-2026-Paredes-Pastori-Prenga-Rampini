package it.polimi.it.cards.character;

import it.polimi.it.enums.CharacterType;
import it.polimi.it.enums.Era;
import it.polimi.it.Player.Player;

/**
 * Character card: Inventor.
 *
 * Each inventor shows exactly one invention icon (10 different icons exist).
 * End of game: (inventor count) × (distinct invention icon count) (rulebook p.7).
 */
public class Inventor extends Character {

    private final String inventionIcon;

    public Inventor(int id, Era era, String inventionIcon) {
        super(id, era, CharacterType.INVENTOR, 1);
        if (inventionIcon == null || inventionIcon.isBlank()) {
            throw new IllegalArgumentException("Invention icon identifier must not be empty.");
        }
        this.inventionIcon = inventionIcon;
    }

    public String getInventionIcon() { return inventionIcon; }

    @Override
    public void applyImmediateEffect(Player player) { /* no immediate effect */ }
}