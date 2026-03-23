package it.polimi.it.cards.character;

import it.polimi.it.cards.Card;
import it.polimi.it.enums.CharacterType;
import it.polimi.it.enums.Era;
import it.polimi.it.player.Player;

/**
 * Character card: the basic unit a player adds to their tribe.
 *
 * Each character has a unique ID, a type, an era, and a number of
 * specialised icons whose meaning depends on the concrete type:
 * - Hunter:  1 if it has the trigger icon, 0 otherwise
 * - Shaman:  number of star icons (1–3)
 * - Inventor: exactly 1 invention icon
 * - Builder/Gatherer/Artist: unused (0)
 *
 * Acquiring a character has no food cost, but during the Sustenance event
 * each character in the tribe must be fed (rulebook p.6).
 */
public abstract class Character extends Card {

    private final CharacterType type;
    private final int           iconCount;

    public Character(int id, Era era, CharacterType type, int iconCount) {
        super(id, era);
        this.type      = type;
        this.iconCount = iconCount;
    }

    /** Returns the type of this character. */
    public CharacterType getType()      { return type; }

    /** Returns the number of specialised icons printed on this card. */
    public int           getIconCount() { return iconCount; }

    /**
     * Applies the character's immediate effect to the player who just acquired it.
     * Most characters have no immediate effect (empty implementation).
     * The exception is the Hunter with the trigger icon (rulebook p.5).
     */
    public abstract void applyImmediateEffect(Player player);
}