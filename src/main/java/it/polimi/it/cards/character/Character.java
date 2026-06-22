package it.polimi.it.cards.character;

import it.polimi.it.cards.Card;
import it.polimi.it.enums.CharacterType;
import it.polimi.it.enums.Era;
import it.polimi.it.player.Player;

/**
 * Base class for character cards.
 *
 * <p>Each character has a unique ID, a type, an era, and a number of
 * specialised icons whose meaning depends on the concrete type. Acquiring a
 * character has no food cost, but each character in the tribe must be fed
 * during the Sustenance event.
 */
public abstract class Character extends Card {

    private final CharacterType type;
    private final int           iconCount;

    public Character(int id, Era era, CharacterType type, int iconCount) {
        super(id, era);
        this.type      = type;
        this.iconCount = iconCount;
    }

    /**
     * Returns the type of this character.
     *
     * @return character type
     */
    public CharacterType getType()      { return type; }

    /**
     * Returns the number of specialised icons printed on this card.
     *
     * @return specialised icon count
     */
    public int           getIconCount() { return iconCount; }

    /**
     * Applies the character's immediate effect to the player who just acquired it.
     * Most characters have no immediate effect; the exception is the Hunter
     * with the trigger icon.
     *
     * @param player player who acquired the character
     */
    public abstract void applyImmediateEffect(Player player);
}
